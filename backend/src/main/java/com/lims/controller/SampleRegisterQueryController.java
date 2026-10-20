package com.lims.controller;

import com.lims.common.R;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 样品管理 - 登记查询（仅已登记 state_register='T' 的 project 数据）。
 * 条件：样品编号区间、样品登记时间区间、产品名称、受检单位、监抽任务编号。
 * 「分解信息」关联 prj_detail（按 project_bh = project.bh）。
 */
@RestController
@RequestMapping("/sample-register-query")
public class SampleRegisterQueryController {

    private static final int PAGE_SIZE = 20;

    private final JdbcTemplate jdbcTemplate;

    public SampleRegisterQueryController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/list")
    public R<Map<String, Object>> list(
            @RequestParam(value = "bhStart", required = false) String bhStart,
            @RequestParam(value = "bhEnd", required = false) String bhEnd,
            @RequestParam(value = "regStart", required = false) String regStart,
            @RequestParam(value = "regEnd", required = false) String regEnd,
            @RequestParam(value = "productName", required = false) String productName,
            @RequestParam(value = "sjUnit", required = false) String sjUnit,
            @RequestParam(value = "taskno", required = false) String taskno,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        List<String> where = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        where.add("p.state_register = 'T'");
        addRange(where, args, "p.bh", bhStart, true);
        addRange(where, args, "p.bh", bhEnd, false);
        addRange(where, args, "p.date_reg", regStart, true);
        addRange(where, args, "p.date_reg", regEnd, false);
        addLike(where, args, "p.productName", productName);
        addLike(where, args, "p.dwname_sj_nj", sjUnit);
        addLike(where, args, "p.taskno", taskno);

        String whereSql = String.join(" AND ", where);

        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project p WHERE " + whereSql,
                Integer.class,
                args.toArray()
        );

        int limit = size <= 0 ? PAGE_SIZE : Math.min(size, 200);
        int pageIndex = Math.max(page, 1);
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(limit);
        pageArgs.add((pageIndex - 1) * limit);

        List<Map<String, Object>> records = jdbcTemplate.query(
                "SELECT p.id_nj, p.taskno, p.bh, p.dwname_sj_nj, p.dwname_sc_nj, "
                        + "p.productName, p.state_div, p.byNum, p.date_reg, p.date_yywc, p.result, "
                        + "(SELECT COUNT(*) FROM prj_detail d WHERE d.project_bh = p.bh) AS detail_count "
                        + "FROM project p WHERE " + whereSql + " "
                        + "ORDER BY p.date_reg DESC, p.id_nj DESC "
                        + "LIMIT ? OFFSET ?",
                this::mapRow,
                pageArgs.toArray()
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total == null ? 0 : total);
        return R.ok(result);
    }

    /**
     * 分解信息：某个样品编号（bh）下的检测单项明细
     */
    @GetMapping("/divide-detail")
    public R<List<Map<String, Object>>> divideDetail(@RequestParam("bh") String bh) {
        if (bh == null || bh.trim().isEmpty()) {
            return R.ok(List.of());
        }
        List<Map<String, Object>> records = jdbcTemplate.query(
                "SELECT item_xh, testItem, basis, mathod, jyResult, item_evaluate "
                        + "FROM prj_detail WHERE project_bh = ? ORDER BY item_xh",
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("itemXh", rs.getInt("item_xh"));
                    row.put("testItem", rs.getString("testItem"));
                    row.put("basis", rs.getString("basis"));
                    row.put("mathod", rs.getString("mathod"));
                    row.put("jyResult", rs.getString("jyResult"));
                    row.put("itemEvaluate", rs.getString("item_evaluate"));
                    return row;
                },
                bh.trim()
        );
        return R.ok(records);
    }

    private void addLike(List<String> where, List<Object> args, String column, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        where.add(column + " LIKE ?");
        args.add("%" + value.trim() + "%");
    }

    /**
     * 区间条件（varchar 日期/编号按字典序比较，与旧系统一致）
     *
     * @param isStart true=大于等于，false=小于等于
     */
    private void addRange(List<String> where, List<Object> args, String column,
                          String value, boolean isStart) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        where.add(column + (isStart ? " >= ?" : " <= ?"));
        args.add(value.trim());
    }

    private Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id_nj", rs.getLong("id_nj"));
        row.put("taskno", rs.getString("taskno"));
        row.put("bh", rs.getString("bh"));
        row.put("dwname_sj_nj", rs.getString("dwname_sj_nj"));
        row.put("dwname_sc_nj", rs.getString("dwname_sc_nj"));
        row.put("productName", rs.getString("productName"));
        row.put("stateDiv", rs.getString("state_div"));
        row.put("byNum", rs.getString("byNum"));
        row.put("date_reg", rs.getString("date_reg"));
        row.put("date_yywc", rs.getString("date_yywc"));
        row.put("result", rs.getString("result"));
        row.put("detailCount", rs.getInt("detail_count"));
        return row;
    }
}
