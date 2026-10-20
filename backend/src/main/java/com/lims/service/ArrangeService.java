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
 * 分解排样 - 任务安排 / 批量安排。
 *
 * 列表口径：
 * - 待安排检验项目：state_div='T'（分解完毕）且 state_arrange<>'T'；
 * - 已安排检验项目：state_arrange='T'。
 *
 * 检验员分配算法（与旧系统一致）：
 * 1. 先按检验项目查「方法检验员」维护的项目负责关系（user_item.item 命中检测项目）；
 * 2. 未命中时按样品类别取方法检验员（user_method.method = 样品编号中的 SA / NA / XA）；
 * 3. 都匹配不到则不分配，由页面提示，避免随意指派。
 * 命中后把检验员写到 prj_detail.userId_jyy（每个检测项目一条），
 * 并按「样品 + 检验员」在 task 表生成检验任务。
 */
@Service
public class ArrangeService {

    private static final String ROW_COLUMNS =
            "p.id_nj, p.bh, p.taskno, p.productName, p.date_yywc, p.state_div, p.state_arrange, "
                    + "(SELECT COUNT(*) FROM prj_detail d WHERE d.project_bh = p.bh) AS detail_count, "
                    + "(SELECT COUNT(*) FROM task t WHERE t.project_bh = p.bh) AS task_count";

    private final JdbcTemplate jdbcTemplate;

    public ArrangeService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 待安排检验项目 */
    public Map<String, Object> pending(String taskno) {
        List<String> where = new ArrayList<>(List.of(
                "p.bh IS NOT NULL",
                "p.state_div = 'T'",
                "COALESCE(p.state_arrange, '') <> 'T'"
        ));
        List<Object> args = new ArrayList<>();
        addLike(where, args, "p.taskno", taskno);

        List<Map<String, Object>> records = jdbcTemplate.query(
                "SELECT " + ROW_COLUMNS + " FROM project p WHERE " + String.join(" AND ", where)
                        + " ORDER BY p.id_nj DESC",
                this::mapRow,
                args.toArray()
        );
        return page(records);
    }

    /** 已安排检验项目 */
    public Map<String, Object> done() {
        List<Map<String, Object>> records = jdbcTemplate.query(
                "SELECT " + ROW_COLUMNS + " FROM project p "
                        + "WHERE p.bh IS NOT NULL AND p.state_arrange = 'T' ORDER BY p.id_nj DESC",
                this::mapRow
        );
        return page(records);
    }

    /** 安排检验任务：单条或批量，按算法分配检验员并生成任务 */
    @Transactional
    public Map<String, Object> assign(List<String> bhs, String taskno) {
        List<Map<String, Object>> samples = locate(bhs, taskno);
        if (samples.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "没有匹配的待安排样品");
        }

        int assigned = 0;
        int skipped = 0;
        List<String> messages = new ArrayList<>();
        for (Map<String, Object> sample : samples) {
            String bh = text(sample.get("bh"));
            List<Map<String, Object>> items = jdbcTemplate.queryForList(
                    "SELECT item_xh, testItem, mathod FROM prj_detail WHERE project_bh = ? ORDER BY item_xh",
                    bh
            );
            if (items.isEmpty()) {
                messages.add(bh + "：尚未分解出检验项目，无法安排");
                continue;
            }

            String methodCode = methodCode(bh);
            List<String> inspectors = new ArrayList<>();
            for (Map<String, Object> item : items) {
                String inspector = resolveInspector(
                        text(item.get("testItem")),
                        text(item.get("mathod")),
                        methodCode
                );
                if (inspector == null) {
                    continue;
                }
                jdbcTemplate.update(
                        "UPDATE prj_detail SET userId_jyy = ? WHERE project_bh = ? AND item_xh = ?",
                        inspector, bh, item.get("item_xh")
                );
                if (!inspectors.contains(inspector)) {
                    inspectors.add(inspector);
                }
            }

            if (inspectors.isEmpty()) {
                messages.add(bh + "：没有匹配到检验员，请在方法检验员中维护");
                continue;
            }
            for (String inspector : inspectors) {
                if (taskExists(bh, inspector)) {
                    skipped++;
                    continue;
                }
                jdbcTemplate.update(
                        "INSERT INTO task (userId_jyy, project_bh, date_jyy, state_jyy) VALUES (?, ?, NULL, 'F')",
                        inspector, bh
                );
            }
            jdbcTemplate.update(
                    "UPDATE project SET state_arrange = 'T' WHERE TRIM(bh) = ?",
                    bh
            );
            assigned++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", samples.size());
        result.put("assigned", assigned);
        result.put("skipped", skipped);
        result.put("messages", messages);
        return result;
    }

    /** 转待安排 */
    @Transactional
    public int revert(List<String> bhs) {
        List<String> targets = normalize(bhs);
        if (targets.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请先勾选需要转待安排的样品");
        }
        String placeholders = String.join(",", targets.stream().map(item -> "?").toList());
        return jdbcTemplate.update(
                "UPDATE project SET state_arrange = 'R' WHERE TRIM(bh) IN (" + placeholders + ")",
                targets.toArray()
        );
    }

    /** 退分解 */
    @Transactional
    public int returnDecompose(List<String> bhs) {
        List<String> targets = normalize(bhs);
        if (targets.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请先勾选需要退分解的样品");
        }
        String placeholders = String.join(",", targets.stream().map(item -> "?").toList());
        return jdbcTemplate.update(
                "UPDATE project SET state_div = 'F', state_arrange = 'F' "
                        + "WHERE TRIM(bh) IN (" + placeholders + ")",
                targets.toArray()
        );
    }

    /** 退登记 */
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

    private List<Map<String, Object>> locate(List<String> bhs, String taskno) {
        List<String> targets = normalize(bhs);
        if (!targets.isEmpty()) {
            String placeholders = String.join(",", targets.stream().map(item -> "?").toList());
            return jdbcTemplate.queryForList(
                    "SELECT id_nj, bh FROM project WHERE TRIM(bh) IN (" + placeholders + ")",
                    targets.toArray()
            );
        }

        List<String> where = new ArrayList<>(List.of(
                "bh IS NOT NULL",
                "state_div = 'T'",
                "COALESCE(state_arrange, '') <> 'T'"
        ));
        List<Object> args = new ArrayList<>();
        addLike(where, args, "taskno", taskno);
        if (args.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请先勾选样品，或填写任务编号");
        }
        return jdbcTemplate.queryForList(
                "SELECT id_nj, bh FROM project WHERE " + String.join(" AND ", where)
                        + " ORDER BY id_nj DESC",
                args.toArray()
        );
    }

    /**
     * 检验员匹配：先按检验项目（user_item），再按样品类别方法码（user_method）。
     * 检测项目在明细里带单位（如「阿维菌素,mg/kg」），取逗号前的主体做匹配。
     */
    private String resolveInspector(String testItem, String mathod, String methodCode) {
        String item = testItem.split("[,，]")[0].trim();
        if (!item.isEmpty()) {
            List<String> byItem = jdbcTemplate.queryForList(
                    "SELECT usergh FROM user_item WHERE item = ? ORDER BY id LIMIT 1",
                    String.class,
                    item
            );
            if (!byItem.isEmpty()) {
                return byItem.get(0);
            }
        }
        if (!methodCode.isEmpty()) {
            List<String> byMethod = jdbcTemplate.queryForList(
                    "SELECT usergh FROM user_method WHERE method = ? ORDER BY id LIMIT 1",
                    String.class,
                    methodCode
            );
            if (!byMethod.isEmpty()) {
                return byMethod.get(0);
            }
        }
        return null;
    }

    /** 样品编号中段即方法码：JK(2023)-SA-425 → SA */
    private static String methodCode(String bh) {
        String[] parts = bh.split("-");
        return parts.length > 1 ? parts[1].trim() : "";
    }

    private boolean taskExists(String bh, String inspector) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM task WHERE project_bh = ? AND userId_jyy = ?",
                Integer.class,
                bh, inspector
        );
        return count != null && count > 0;
    }

    private void addLike(List<String> where, List<Object> args, String column, String value) {
        String search = text(value);
        if (search.isEmpty()) {
            return;
        }
        where.add(column + " LIKE ?");
        args.add("%" + search + "%");
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
        row.put("state_arrange", rs.getString("state_arrange"));
        row.put("detail_count", rs.getInt("detail_count"));
        row.put("task_count", rs.getInt("task_count"));
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
