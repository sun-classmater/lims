package com.lims.service;

import com.lims.common.BusinessException;
import com.lims.common.ResultCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 分解排样 - 样品分解。
 *
 * 分解 = 按样品的标准库（project.testLib → product.id）把 lib 表的检验项目
 * 复制成该样品的检验明细（prj_detail），置 state_div='T' 表示分解完毕。
 *
 * 列表口径：
 * - 待分解样品检验项目：state_div <> 'T'（尚未确认分解完毕）；
 * - 已分解样品检验项目：state_div = 'T'；
 * - 分解状态列：该样品是否已生成检验明细（prj_detail 有无记录）。
 */
@Service
public class DecomposeService {

    private static final String ROW_COLUMNS =
            "p.id_nj, p.bh, p.taskno, p.productName, p.date_yywc, p.state_div, p.testLib, "
                    + "(SELECT COUNT(*) FROM prj_detail d WHERE d.project_bh = p.bh) AS detail_count";

    private final JdbcTemplate jdbcTemplate;

    public DecomposeService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 待分解样品：未确认分解完毕的样品 */
    public Map<String, Object> pending(String taskno, String productName) {
        List<String> where = new ArrayList<>(List.of(
                "p.bh IS NOT NULL",
                "COALESCE(p.state_div, '') <> 'T'"
        ));
        List<Object> args = new ArrayList<>();
        addLike(where, args, "p.taskno", taskno);
        addLike(where, args, "p.productName", productName);

        List<Map<String, Object>> records = jdbcTemplate.query(
                "SELECT " + ROW_COLUMNS + " FROM project p WHERE " + String.join(" AND ", where)
                        + " ORDER BY p.id_nj DESC",
                this::mapRow,
                args.toArray()
        );
        return page(records);
    }

    /** 已分解样品：分解完毕的样品 */
    public Map<String, Object> done() {
        List<Map<String, Object>> records = jdbcTemplate.query(
                "SELECT " + ROW_COLUMNS + " FROM project p "
                        + "WHERE p.bh IS NOT NULL AND p.state_div = 'T' ORDER BY p.id_nj DESC",
                this::mapRow
        );
        return page(records);
    }

    /**
     * 批量分解：按勾选的样品，或按任务号/样品名称定位样品，按标准库生成检验明细。
     * 已经生成过明细的样品直接跳过，不覆盖已有数据。
     */
    @Transactional
    public Map<String, Object> generate(
            List<String> bhs,
            String library,
            String taskno,
            String keyword
    ) {
        String explicitLibrary = resolveExplicitLibrary(library);
        List<Map<String, Object>> samples = locateSamples(bhs, taskno, keyword);
        if (samples.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "没有匹配的样品可分解");
        }

        int generated = 0;
        int skipped = 0;
        List<String> messages = new ArrayList<>();
        for (Map<String, Object> sample : samples) {
            String bh = text(sample.get("bh"));
            String libId = explicitLibrary != null ? explicitLibrary : text(sample.get("testLib"));
            if (libId.isEmpty()) {
                messages.add(bh + "：未指定标准库，无法分解");
                continue;
            }
            if (countDetails(bh) > 0) {
                skipped++;
                continue;
            }
            if (insertDetails(bh, libId) > 0) {
                generated++;
            } else {
                messages.add(bh + "：标准库 " + libId + " 下没有检验项目");
            }
        }
        return result(samples.size(), generated, skipped, messages);
    }

    /** 分解完毕：必要时先生成检验明细，然后置 state_div='T' */
    @Transactional
    public Map<String, Object> complete(List<String> bhs, String library) {
        List<String> targets = normalize(bhs);
        if (targets.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请先勾选需要确认的样品");
        }
        String explicitLibrary = resolveExplicitLibrary(library);

        int completed = 0;
        int generated = 0;
        List<String> messages = new ArrayList<>();
        for (String bh : targets) {
            if (countDetails(bh) == 0) {
                String libId = explicitLibrary != null ? explicitLibrary : sampleLibrary(bh);
                if (libId.isEmpty()) {
                    messages.add(bh + "：尚未分解，且该样品未指定标准库");
                    continue;
                }
                if (insertDetails(bh, libId) == 0) {
                    messages.add(bh + "：标准库 " + libId + " 下没有检验项目");
                    continue;
                }
                generated++;
            }
            // 分解完毕即进入待安排；已安排过的样品保持其安排状态
            jdbcTemplate.update(
                    "UPDATE project SET state_div = 'T', "
                            + "state_arrange = CASE WHEN COALESCE(state_arrange, '') = 'T' "
                            + "THEN state_arrange ELSE 'R' END "
                            + "WHERE TRIM(bh) = ?",
                    bh
            );
            completed++;
        }
        Map<String, Object> summary = result(targets.size(), generated, 0, messages);
        summary.put("completed", completed);
        return summary;
    }

    /**
     * 转待分解：退回 state_div='F'。
     * 已经安排过检验任务的样品不再回退，避免留下无主的检验任务。
     */
    @Transactional
    public Map<String, Object> revert(List<String> bhs) {
        List<String> targets = normalize(bhs);
        if (targets.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请先勾选需要转待分解的样品");
        }
        int reverted = 0;
        List<String> messages = new ArrayList<>();
        for (String bh : targets) {
            List<String> arranged = jdbcTemplate.queryForList(
                    "SELECT bh FROM project WHERE TRIM(bh) = ? AND COALESCE(state_arrange, '') = 'T'",
                    String.class,
                    bh
            );
            if (!arranged.isEmpty()) {
                messages.add(bh + "：已安排检验任务，请先在任务安排页转待安排");
                continue;
            }
            reverted += jdbcTemplate.update(
                    "UPDATE project SET state_div = 'F', state_arrange = 'F' WHERE TRIM(bh) = ?",
                    bh
            );
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", targets.size());
        result.put("reverted", reverted);
        result.put("messages", messages);
        return result;
    }

    /** 退登记：退回登记状态 */
    @Transactional
    public int returnToRegister(List<String> bhs) {
        List<String> targets = normalize(bhs);
        if (targets.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请先勾选需要退登记的样品");
        }
        String placeholders = String.join(",", targets.stream().map(item -> "?").toList());
        return jdbcTemplate.update(
                "UPDATE project SET state_register = 'F' WHERE TRIM(bh) IN (" + placeholders + ")",
                targets.toArray()
        );
    }

    private List<Map<String, Object>> locateSamples(
            List<String> bhs,
            String taskno,
            String keyword
    ) {
        List<String> targets = normalize(bhs);
        if (!targets.isEmpty()) {
            String placeholders = String.join(",", targets.stream().map(item -> "?").toList());
            return jdbcTemplate.queryForList(
                    "SELECT id_nj, bh, testLib FROM project WHERE TRIM(bh) IN (" + placeholders + ")",
                    targets.toArray()
            );
        }

        List<String> where = new ArrayList<>(List.of(
                "bh IS NOT NULL",
                "COALESCE(state_div, '') <> 'T'"
        ));
        List<Object> args = new ArrayList<>();
        addLike(where, args, "taskno", taskno);
        String search = text(keyword);
        if (!search.isEmpty()) {
            where.add("(taskno LIKE ? OR productName LIKE ?)");
            args.add("%" + search + "%");
            args.add("%" + search + "%");
        }
        if (args.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请先勾选样品，或填写任务号/样品名称");
        }
        return jdbcTemplate.queryForList(
                "SELECT id_nj, bh, testLib FROM project WHERE " + String.join(" AND ", where)
                        + " ORDER BY id_nj DESC",
                args.toArray()
        );
    }

    /** 按标准库生成检验明细；返回写入条数 */
    private int insertDetails(String bh, String libraryId) {
        return jdbcTemplate.update(
                "INSERT INTO prj_detail (project_bh, item_xh, testItem, basis, mathod, stdValue, "
                        + "num_lowerst, method_note) "
                        + "SELECT ?, xh, testItem, basis, mathod, stdValue, num_lowerst, method_note "
                        + "FROM lib WHERE productId = ? AND testItem IS NOT NULL ORDER BY xh",
                bh, libraryId
        );
    }

    private int countDetails(String bh) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM prj_detail WHERE project_bh = ?",
                Integer.class,
                bh
        );
        return count == null ? 0 : count;
    }

    private String sampleLibrary(String bh) {
        List<String> values = jdbcTemplate.queryForList(
                "SELECT testLib FROM project WHERE TRIM(bh) = ?",
                String.class,
                bh
        );
        return values.isEmpty() ? "" : text(values.get(0));
    }

    /**
     * 页面填写标准库时的解析：空值或 all 表示用样品自身的标准库；
     * 否则按项目库编号或名称查找，找不到直接报错，避免静默失败。
     */
    private String resolveExplicitLibrary(String library) {
        String value = text(library);
        if (value.isEmpty() || "all".equalsIgnoreCase(value)) {
            return null;
        }
        List<String> ids = jdbcTemplate.queryForList(
                "SELECT id FROM product WHERE id = ? OR libName = ? ORDER BY id LIMIT 1",
                String.class,
                value,
                value
        );
        if (ids.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "标准库 " + value + " 不存在，请填写项目库编号或名称");
        }
        return ids.get(0);
    }

    private void addLike(List<String> where, List<Object> args, String column, String value) {
        String search = text(value);
        if (search.isEmpty()) {
            return;
        }
        where.add(column + " LIKE ?");
        args.add("%" + search + "%");
    }

    private Map<String, Object> result(
            int total,
            int generated,
            int skipped,
            List<String> messages
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("generated", generated);
        result.put("skipped", skipped);
        result.put("messages", messages);
        return result;
    }

    private Map<String, Object> page(List<Map<String, Object>> records) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", records.size());
        return result;
    }

    private Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id_nj", rs.getLong("id_nj"));
        row.put("bh", rs.getString("bh"));
        row.put("taskno", rs.getString("taskno"));
        row.put("productName", rs.getString("productName"));
        row.put("date_yywc", rs.getString("date_yywc"));
        row.put("state_div", rs.getString("state_div"));
        row.put("testLib", rs.getString("testLib"));
        row.put("detail_count", rs.getInt("detail_count"));
        return row;
    }

    private static List<String> normalize(List<String> raw) {
        List<String> values = new ArrayList<>();
        if (raw == null) {
            return values;
        }
        for (String item : raw) {
            String value = text(item);
            if (!value.isEmpty() && !values.contains(value)) {
                values.add(value);
            }
        }
        return values;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
