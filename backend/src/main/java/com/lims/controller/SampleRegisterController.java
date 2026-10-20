package com.lims.controller;

import com.lims.common.BusinessException;
import com.lims.common.R;
import com.lims.common.ResultCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 样品管理 - 样品登记信息维护与确认。
 * 数据来自 project 表；登记人 userId_reg 存的是 user.gh，展示时关联 user.name。
 * 左侧：待登记（state_register='F'）；右侧：已确认登记样品（state_register='T'）。
 */
@RestController
@RequestMapping("/sample-register")
public class SampleRegisterController {

    private static final int PAGE_SIZE = 10;

    private final JdbcTemplate jdbcTemplate;

    public SampleRegisterController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 待登记样品列表（state_register='F'），可按任务编号/样品编号查找
     */
    @GetMapping("/pending")
    public R<Map<String, Object>> pending(
            @RequestParam(value = "taskno", required = false) String taskno,
            @RequestParam(value = "bh", required = false) String bh,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        List<String> where = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        where.add("p.state_register = 'F'");
        addLike(where, args, "p.taskno", taskno);
        addLike(where, args, "p.bh", bh);

        String whereSql = String.join(" AND ", where);
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project p WHERE " + whereSql,
                Integer.class,
                args.toArray()
        );

        int limit = size <= 0 ? PAGE_SIZE : Math.min(size, 100);
        int pageIndex = Math.max(page, 1);
        List<Map<String, Object>> records = jdbcTemplate.query(
                "SELECT p.id_nj, p.taskno, p.bh, p.dwname_sj_nj, p.productName, "
                        + "p.date_reg, p.userId_reg, u.name AS register_name, "
                        + "p.ggxh, p.ypsb, p.ypdj, p.ypNum, p.byNum, p.cyjs, p.cyaddr, "
                        + "p.ypCharacter, p.ypSituation, p.ypSaveReq, p.yyProcess, p.ypbz "
                        + "FROM project p LEFT JOIN user u ON u.gh = p.userId_reg "
                        + "WHERE " + whereSql + " "
                        + "ORDER BY (p.date_reg IS NULL), p.date_reg DESC, p.id_nj DESC "
                        + "LIMIT ? OFFSET ?",
                this::mapPendingRow,
                appendPaging(args, limit, pageIndex)
        );

        return R.ok(pageResult(records, total == null ? 0 : total));
    }

    /**
     * 已确认登记样品列表（state_register='T'），可按项目编号搜索
     */
    @GetMapping("/registered")
    public R<Map<String, Object>> registered(
            @RequestParam(value = "bh", required = false) String bh,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        List<String> where = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        where.add("p.state_register = 'T'");
        addLike(where, args, "p.bh", bh);

        String whereSql = String.join(" AND ", where);
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project p WHERE " + whereSql,
                Integer.class,
                args.toArray()
        );

        int limit = size <= 0 ? PAGE_SIZE : Math.min(size, 100);
        int pageIndex = Math.max(page, 1);
        List<Map<String, Object>> records = jdbcTemplate.query(
                "SELECT p.id_nj, p.taskno, p.bh, p.productName, p.date_yywc, "
                        + "p.userId_reg, u.name AS register_name "
                        + "FROM project p LEFT JOIN user u ON u.gh = p.userId_reg "
                        + "WHERE " + whereSql + " "
                        + "ORDER BY p.date_reg DESC, p.id_nj DESC "
                        + "LIMIT ? OFFSET ?",
                this::mapRegisteredRow,
                appendPaging(args, limit, pageIndex)
        );

        return R.ok(pageResult(records, total == null ? 0 : total));
    }

    /**
     * 确认登记【登记完毕】：支持单条与批量。
     * state_register 置 T；登记日期/登记人缺失时补当前用户与当天日期。
     */
    @PostMapping("/confirm")
    @Transactional
    public R<Map<String, Object>> confirm(
            @RequestBody ConfirmRequest request,
            @RequestAttribute(value = "userId", required = false) String userId
    ) {
        if (request == null || request.ids() == null || request.ids().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择需要确认登记的样品");
        }

        String placeholders = String.join(",", request.ids().stream().map(id -> "?").toList());
        String today = LocalDate.now().toString();
        Object[] args = new Object[request.ids().size() + 2];
        args[0] = today;
        args[1] = userId;
        for (int i = 0; i < request.ids().size(); i++) {
            args[i + 2] = request.ids().get(i);
        }

        int affected = jdbcTemplate.update(
                "UPDATE project SET state_register = 'T', "
                        + "date_reg = CASE WHEN date_reg IS NULL OR date_reg = '' THEN ? ELSE date_reg END, "
                        + "userId_reg = CASE WHEN userId_reg IS NULL OR userId_reg = '' THEN ? ELSE userId_reg END "
                        + "WHERE id_nj IN (" + placeholders + ") AND state_register <> 'T'",
                args
        );

        return R.ok(Map.of("affected", affected));
    }

    /**
     * 转待登记：将已确认的样品退回待登记状态
     */
    @PostMapping("/revert")
    @Transactional
    public R<Map<String, Object>> revert(@RequestBody RevertRequest request) {
        if (request == null || request.id() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "缺少要转待登记的样品");
        }

        int affected = jdbcTemplate.update(
                "UPDATE project SET state_register = 'F' WHERE id_nj = ? AND state_register = 'T'",
                request.id()
        );
        if (affected == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该样品不是已登记状态，无法转待登记");
        }
        return R.ok(Map.of("affected", affected));
    }

    private void addLike(List<String> where, List<Object> args, String column, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        where.add(column + " LIKE ?");
        args.add("%" + value.trim() + "%");
    }

    private Object[] appendPaging(List<Object> args, int limit, int page) {
        List<Object> all = new ArrayList<>(args);
        all.add(limit);
        all.add((page - 1) * limit);
        return all.toArray();
    }

    private Map<String, Object> pageResult(List<Map<String, Object>> records, int total) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        return result;
    }

    private Map<String, Object> mapPendingRow(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id_nj", rs.getLong("id_nj"));
        row.put("taskno", rs.getString("taskno"));
        row.put("bh", rs.getString("bh"));
        row.put("dwname_sj_nj", rs.getString("dwname_sj_nj"));
        row.put("productName", rs.getString("productName"));
        row.put("date_reg", rs.getString("date_reg"));
        row.put("registerName", resolveName(rs));
        row.put("ggxh", rs.getString("ggxh"));
        row.put("ypsb", rs.getString("ypsb"));
        row.put("ypdj", rs.getString("ypdj"));
        row.put("ypNum", rs.getString("ypNum"));
        row.put("byNum", rs.getString("byNum"));
        row.put("cyjs", rs.getString("cyjs"));
        row.put("cyaddr", rs.getString("cyaddr"));
        row.put("ypCharacter", rs.getString("ypCharacter"));
        row.put("ypSituation", rs.getString("ypSituation"));
        row.put("ypSaveReq", rs.getString("ypSaveReq"));
        row.put("yyProcess", rs.getString("yyProcess"));
        row.put("ypbz", rs.getString("ypbz"));
        return row;
    }

    private Map<String, Object> mapRegisteredRow(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id_nj", rs.getLong("id_nj"));
        row.put("taskno", rs.getString("taskno"));
        row.put("bh", rs.getString("bh"));
        row.put("productName", rs.getString("productName"));
        row.put("date_yywc", rs.getString("date_yywc"));
        row.put("registerName", resolveName(rs));
        return row;
    }

    private String resolveName(ResultSet rs) throws SQLException {
        String name = rs.getString("register_name");
        if (name != null && !name.isBlank()) {
            return name;
        }
        String gh = rs.getString("userId_reg");
        return (gh == null || gh.isBlank()) ? "" : gh;
    }

    public record ConfirmRequest(List<Long> ids) {
    }

    public record RevertRequest(Long id) {
    }
}
