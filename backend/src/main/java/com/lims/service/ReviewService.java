package com.lims.service;

import com.lims.common.BusinessException;
import com.lims.common.ResultCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 审核签发 - 报告签发 / 检验汇总查询。
//
// 报告签发：检验已完成（state_comlete='T'）且尚未签发的项目；
// 签发写入 state_verify='T' 与签发日期（date_stamp）。
// 检验汇总查询：按检测项目展开检验数据，带检验员、完成时间、签发与盖章时间。
@Service
public class ReviewService {

    private static final String PENDING_COLUMNS =
            "p.id_nj, p.bh, p.taskno, p.productName, p.result, p.date_jycomplete, "
                    + "p.state_verify, COALESCE(NULLIF(c.company, ''), p.dwname_sj_nj) AS client_name";

    private final JdbcTemplate jdbcTemplate;

    public ReviewService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 待授权签发项目
    public Map<String, Object> pending(Map<String, String> filters) {
        List<String> where = new ArrayList<>(List.of(
                "p.bh IS NOT NULL",
                "p.state_comlete = 'T'",
                "COALESCE(p.state_verify, '') <> 'T'"
        ));
        List<Object> args = new ArrayList<>();

        addRange(where, args, "p.bh", text(filters.get("bhStart")), true);
        addRange(where, args, "p.bh", text(filters.get("bhEnd")), false);
        addLike(where, args, "p.productName", text(filters.get("productName")));
        String client = text(filters.get("clientUnit"));
        if (!client.isEmpty()) {
            where.add("(c.company LIKE ? OR p.dwname_sj_nj LIKE ?)");
            args.add("%" + client + "%");
            args.add("%" + client + "%");
        }
        addRange(where, args, "p.date_jycomplete", text(filters.get("finishStart")), true);
        addRange(where, args, "p.date_jycomplete", text(filters.get("finishEnd")), false);
        addLike(where, args, "p.taskno", text(filters.get("taskno")));
        addLike(where, args, "p.taskbatchno", text(filters.get("batchNo")));

        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT " + PENDING_COLUMNS + " FROM project p "
                        + "LEFT JOIN customer c ON c.id = p.customerId "
                        + "WHERE " + String.join(" AND ", where)
                        + " ORDER BY p.id_nj DESC",
                args.toArray()
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", records.size());
        return result;
    }

    // 签发：写入签发状态与签发日期
    @Transactional
    public int issue(List<String> bhs) {
        List<String> targets = normalize(bhs);
        if (targets.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请先勾选需要签发的项目");
        }
        String placeholders = String.join(",", targets.stream().map(item -> "?").toList());
        List<Object> args = new ArrayList<>();
        args.add(LocalDate.now().toString());
        args.addAll(targets);
        return jdbcTemplate.update(
                "UPDATE project SET state_verify = 'T', "
                        + "date_stamp = CASE WHEN date_stamp IS NULL OR date_stamp = '' THEN ? ELSE date_stamp END "
                        + "WHERE TRIM(bh) IN (" + placeholders + ") "
                        + "AND state_comlete = 'T' AND COALESCE(state_verify, '') <> 'T'",
                args.toArray()
        );
    }

    // 退登记
    @Transactional
    public int returnToRegister(List<String> bhs) {
        List<String> targets = normalize(bhs);
        if (targets.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请先勾选需要退登记的项目");
        }
        String placeholders = String.join(",", targets.stream().map(item -> "?").toList());
        return jdbcTemplate.update(
                "UPDATE project SET state_register = 'F' WHERE TRIM(bh) IN (" + placeholders + ")",
                targets.toArray()
        );
    }

    // 检验任务完成汇总查询：一行一个检测项目
    public Map<String, Object> summary(Map<String, String> filters) {
        List<String> where = new ArrayList<>(List.of("d.project_bh IS NOT NULL"));
        List<Object> args = new ArrayList<>();

        String inspector = text(filters.get("inspector"));
        if (!inspector.isEmpty()) {
            where.add("(u.name LIKE ? OR d.userId_jyy LIKE ?)");
            args.add("%" + inspector + "%");
            args.add("%" + inspector + "%");
        }
        addLike(where, args, "d.testItem", text(filters.get("testItem")));
        addRange(where, args, "t.date_jyy", text(filters.get("finishStart")), true);
        addRange(where, args, "t.date_jyy", text(filters.get("finishEnd")), false);
        addRange(where, args, "p.date_stamp", text(filters.get("issueStart")), true);
        addRange(where, args, "p.date_stamp", text(filters.get("issueEnd")), false);
        addRange(where, args, "p.date_printreport", text(filters.get("stampStart")), true);
        addRange(where, args, "p.date_printreport", text(filters.get("stampEnd")), false);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT u.name AS inspector_name, d.userId_jyy, d.project_bh, d.item_xh, "
                        + "d.testItem, d.jyResult, d.item_evaluate, d.jyResult_note, "
                        + "t.date_jyy AS finish_date, p.date_stamp AS issue_date, "
                        + "p.date_printreport AS stamp_date "
                        + "FROM prj_detail d "
                        + "JOIN project p ON TRIM(p.bh) = TRIM(d.project_bh) "
                        + "LEFT JOIN `user` u ON u.gh = d.userId_jyy "
                        + "LEFT JOIN task t ON TRIM(t.project_bh) = TRIM(d.project_bh) "
                        + "AND t.userId_jyy = d.userId_jyy "
                        + "WHERE " + String.join(" AND ", where)
                        + " ORDER BY d.project_bh, d.item_xh LIMIT 500",
                args.toArray()
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", records.size());
        return result;
    }

    private void addLike(List<String> where, List<Object> args, String column, String value) {
        if (value.isEmpty()) {
            return;
        }
        where.add(column + " LIKE ?");
        args.add("%" + value + "%");
    }

    private void addRange(
            List<String> where,
            List<Object> args,
            String column,
            String value,
            boolean start
    ) {
        if (value.isEmpty()) {
            return;
        }
        where.add(column + (start ? " >= ?" : " <= ?"));
        args.add(value);
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
