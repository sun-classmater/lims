package com.lims.service;

import com.lims.common.BusinessException;
import com.lims.common.ResultCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 项目检验 - 任务查询 / 数据录入。
 *
 * 列表口径（task 表即分解排样生成的检验任务）：
 * - 待检验 / 待录入：state_jyy <> 'T'，且样品仍在安排状态（state_arrange='T'）；
 * - 已检验 / 已录入：state_jyy = 'T'。
 *
 * 数据录入把检验结果写回 prj_detail（jyResult / item_evaluate / jyResult_note）；
 * 标记完成时要求该样品的全部检验项目都已录入结果，并更新 task 状态与完成日期。
 */
@Service
public class InspectService {

    private static final String TASK_COLUMNS =
            "t.id, t.userId_jyy, t.state_jyy, t.date_jyy, "
                    + "p.bh, p.taskno, p.productName, p.date_yywc, p.date_sy, p.date_cy, "
                    + "s.xiaddate AS issue_date";

    private static final String TASK_JOINS =
            " FROM task t "
                    + "JOIN project p ON TRIM(p.bh) = TRIM(t.project_bh) "
                    + "LEFT JOIN sjtask s ON TRIM(s.taskno) = TRIM(p.taskno) ";

    private final JdbcTemplate jdbcTemplate;

    public InspectService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 待检验检验任务列表 */
    public Map<String, Object> pendingTasks(boolean mine, String userId) {
        List<String> where = new ArrayList<>(List.of(
                "COALESCE(t.state_jyy, '') <> 'T'",
                "COALESCE(p.state_arrange, '') = 'T'"
        ));
        List<Object> args = new ArrayList<>();
        if (mine) {
            where.add("t.userId_jyy = ?");
            args.add(userId);
        }
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT " + TASK_COLUMNS + TASK_JOINS + "WHERE " + String.join(" AND ", where)
                        + " ORDER BY t.id DESC",
                args.toArray()
        );
        return page(records);
    }

    /** 已检验检验任务列表 */
    public Map<String, Object> doneTasks(boolean mine, String userId) {
        List<String> where = new ArrayList<>(List.of("t.state_jyy = 'T'"));
        List<Object> args = new ArrayList<>();
        if (mine) {
            where.add("t.userId_jyy = ?");
            args.add(userId);
        }
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT " + TASK_COLUMNS + TASK_JOINS + "WHERE " + String.join(" AND ", where)
                        + " ORDER BY t.date_jyy DESC, t.id DESC",
                args.toArray()
        );
        return page(records);
    }

    /** 指定样品的检验项目明细 */
    public List<Map<String, Object>> items(List<String> bhs) {
        List<String> targets = normalize(bhs);
        if (targets.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", targets.stream().map(item -> "?").toList());
        return jdbcTemplate.queryForList(
                "SELECT d.project_bh, p.productName, p.taskno, d.item_xh, d.testItem, d.basis, "
                        + "d.mathod, d.stdValue, d.jyResult, d.jyResult_note, d.item_evaluate, "
                        + "d.userId_jyy, u.name AS inspector_name "
                        + "FROM prj_detail d "
                        + "JOIN project p ON TRIM(p.bh) = TRIM(d.project_bh) "
                        + "LEFT JOIN `user` u ON u.gh = d.userId_jyy "
                        + "WHERE d.project_bh IN (" + placeholders + ") "
                        + "ORDER BY d.project_bh, d.item_xh",
                targets.toArray()
        );
    }

    /**
     * 保存检验结果。
     * complete=true 时要求样品下所有项目都已录入结果，然后标记任务完成。
     */
    @Transactional
    public Map<String, Object> saveResults(List<ResultItem> items, boolean complete) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "没有需要保存的检验数据");
        }

        int saved = 0;
        Set<String> touched = new LinkedHashSet<>();
        for (ResultItem item : items) {
            String bh = text(item.project_bh());
            if (bh.isEmpty() || item.item_xh() == null) {
                continue;
            }
            saved += jdbcTemplate.update(
                    "UPDATE prj_detail SET jyResult = ?, item_evaluate = ?, jyResult_note = ? "
                            + "WHERE project_bh = ? AND item_xh = ?",
                    blankToNull(item.jyResult()),
                    blankToNull(item.item_evaluate()),
                    blankToNull(item.jyResult_note()),
                    bh,
                    item.item_xh()
            );
            touched.add(bh);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("saved", saved);
        result.put("completed", 0);
        result.put("messages", new ArrayList<String>());

        if (!complete) {
            return result;
        }

        List<String> messages = new ArrayList<>();
        List<String> completedBhs = new ArrayList<>();
        for (String bh : touched) {
            int missing = missingResultCount(bh);
            if (missing > 0) {
                messages.add(bh + "：还有 " + missing + " 个检验项目未录入结果");
                continue;
            }
            completedBhs.add(bh);
        }
        if (completedBhs.isEmpty()) {
            result.put("messages", messages);
            return result;
        }

        String today = java.time.LocalDate.now().toString();
        String placeholders = String.join(",", completedBhs.stream().map(item -> "?").toList());
        List<Object> updateArgs = new ArrayList<>();
        updateArgs.add(today);
        updateArgs.addAll(completedBhs);
        int completed = jdbcTemplate.update(
                "UPDATE task SET state_jyy = 'T', "
                        + "date_jyy = CASE WHEN date_jyy IS NULL OR date_jyy = '' THEN ? ELSE date_jyy END "
                        + "WHERE TRIM(project_bh) IN (" + placeholders + ")",
                updateArgs.toArray()
        );

        result.put("completed", completed);
        result.put("messages", messages);
        return result;
    }

    /** 转待检验：任务回到未完成 */
    @Transactional
    public int revert(List<String> bhs) {
        List<String> targets = normalize(bhs);
        if (targets.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请先勾选需要转待检验的样品");
        }
        String placeholders = String.join(",", targets.stream().map(item -> "?").toList());
        return jdbcTemplate.update(
                "UPDATE task SET state_jyy = 'F', date_jyy = NULL "
                        + "WHERE TRIM(project_bh) IN (" + placeholders + ")",
                targets.toArray()
        );
    }

    /** 退安排：样品退回安排环节 */
    @Transactional
    public int returnArrange(List<String> bhs) {
        List<String> targets = normalize(bhs);
        if (targets.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请先勾选需要退安排的样品");
        }
        String placeholders = String.join(",", targets.stream().map(item -> "?").toList());
        return jdbcTemplate.update(
                "UPDATE project SET state_arrange = 'R' WHERE TRIM(bh) IN (" + placeholders + ")",
                targets.toArray()
        );
    }

    private int missingResultCount(String bh) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM prj_detail WHERE project_bh = ? "
                        + "AND (jyResult IS NULL OR jyResult = '') "
                        + "AND (item_evaluate IS NULL OR item_evaluate = '')",
                Integer.class,
                bh
        );
        return count == null ? 0 : count;
    }

    private static String blankToNull(String value) {
        String text = value == null ? "" : value.trim();
        return text.isEmpty() ? null : text;
    }

    private Map<String, Object> page(List<Map<String, Object>> records) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", records.size());
        return result;
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

    public record ResultItem(
            String project_bh,
            Integer item_xh,
            String jyResult,
            String item_evaluate,
            String jyResult_note
    ) {
    }
}
