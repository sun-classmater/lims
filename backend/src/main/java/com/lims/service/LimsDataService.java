package com.lims.service;

import com.lims.common.BusinessException;
import com.lims.common.PageResult;
import com.lims.common.ResultCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LimsDataService {

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, TableDefinition> definitions = new HashMap<>();
    private final Map<String, List<String>> columnsCache = new HashMap<>();

    public LimsDataService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        registerDefinitions();
    }

    public PageResult<Map<String, Object>> list(
            String entity,
            Map<String, String> filters,
            int page,
            int size,
            String keyword
    ) {
        TableDefinition definition = requireDefinition(entity);
        List<String> columns = getColumns(definition.name);
        Set<String> columnSet = new HashSet<>(columns);

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();

        appendConditions(where, args, definition, filters, keyword);

        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `" + definition.name + "`" + where,
                Long.class,
                args.toArray()
        );

        int safePage = Math.max(page, 1);
        int safeSize = Math.max(Math.min(size, 200), 1);
        int offset = (safePage - 1) * safeSize;

        StringBuilder sql = new StringBuilder("SELECT * FROM `")
                .append(definition.name)
                .append("`")
                .append(where)
                .append(" ORDER BY ");
        for (int i = 0; i < definition.keyColumns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("`").append(definition.keyColumns.get(i)).append("`");
        }
        sql.append(" LIMIT ? OFFSET ?");
        args.add(safeSize);
        args.add(offset);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql.toString(), args.toArray());
        return new PageResult<>(records, total);
    }

    public List<Map<String, Object>> export(
            String entity,
            Map<String, String> filters,
            String keyword
    ) {
        TableDefinition definition = requireDefinition(entity);
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();

        appendConditions(where, args, definition, filters, keyword);

        return jdbcTemplate.queryForList(
                "SELECT * FROM `" + definition.name + "`" + where + " ORDER BY " + definition.keyColumns.get(0),
                args.toArray()
        );
    }

    /**
     * 统一组装查询条件：
     * - likeColumns：模糊匹配 (LIKE '%v%')
     * - filterColumns：精确匹配 (=)
     * - xxxNe：不等值匹配 (&lt;&gt;)，xxx 必须在 filterColumns 白名单中
     * - xxxStart / xxxEnd：区间条件 (&gt;= / &lt;=)，xxx 必须是该实体允许查询的列
     */
    private void appendConditions(
            StringBuilder where,
            List<Object> args,
            TableDefinition definition,
            Map<String, String> filters,
            String keyword
    ) {
        if (filters != null) {
            filters.forEach((key, value) -> {
                if (value == null || value.isBlank()) {
                    return;
                }
                if (definition.likeColumns.contains(key)) {
                    where.append(" AND `").append(key).append("` LIKE ?");
                    args.add("%" + value + "%");
                } else if (definition.filterColumns.contains(key)) {
                    where.append(" AND `").append(key).append("` = ?");
                    args.add(value);
                } else if (key.endsWith("Ne") && definition.filterColumns.contains(key.substring(0, key.length() - 2))) {
                    String column = key.substring(0, key.length() - 2);
                    where.append(" AND `").append(column).append("` <> ?");
                    args.add(value);
                } else if (key.endsWith("Start") || key.endsWith("End")) {
                    String column = key.substring(0, key.length() - (key.endsWith("Start") ? 5 : 3));
                    boolean allowed = definition.likeColumns.contains(column)
                            || definition.filterColumns.contains(column)
                            || definition.searchColumns.contains(column);
                    if (allowed) {
                        String expression = definition.dateVarcharColumns.contains(column)
                                ? "STR_TO_DATE(`" + column + "`, '%Y.%c.%e')"
                                : "`" + column + "`";
                        where.append(" AND ")
                                .append(expression)
                                .append(key.endsWith("Start") ? " >= ?" : " <= ?");
                        args.add(value);
                    }
                }
            });
        }

        if (keyword != null && !keyword.isBlank() && !definition.searchColumns.isEmpty()) {
            where.append(" AND (");
            for (int i = 0; i < definition.searchColumns.size(); i++) {
                if (i > 0) {
                    where.append(" OR ");
                }
                where.append("`")
                        .append(definition.searchColumns.get(i))
                        .append("` LIKE ?");
                args.add("%" + keyword + "%");
            }
            where.append(")");
        }
    }

    public int create(String entity, Map<String, Object> body) {
        TableDefinition definition = requireDefinition(entity);
        Set<String> allowedColumns = new HashSet<>(getColumns(definition.name));
        Map<String, Object> data = filterData(body, allowedColumns);
        if (data.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "没有可写入的字段");
        }

        StringBuilder sql = new StringBuilder("INSERT INTO `")
                .append(definition.name)
                .append("` (");
        List<Object> args = new ArrayList<>();
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                sql.append(", ");
            }
            sql.append("`").append(entry.getKey()).append("`");
            first = false;
        }
        sql.append(") VALUES (");
        for (int i = 0; i < data.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
            args.add(data.values().toArray()[i]);
        }
        sql.append(")");

        return jdbcTemplate.update(sql.toString(), args.toArray());
    }

    public int update(String entity, Map<String, Object> body) {
        TableDefinition definition = requireDefinition(entity);
        Set<String> allowedColumns = new HashSet<>(getColumns(definition.name));
        Map<String, Object> data = filterData(body, allowedColumns);

        for (String key : definition.keyColumns) {
            if (data.get(key) == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "缺少主键字段: " + key);
            }
        }

        List<Object> args = new ArrayList<>();
        StringBuilder setSql = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (definition.keyColumns.contains(entry.getKey())) {
                continue;
            }
            if (!first) {
                setSql.append(", ");
            }
            setSql.append("`").append(entry.getKey()).append("` = ?");
            args.add(entry.getValue());
            first = false;
        }

        if (args.isEmpty()) {
            return 0;
        }

        StringBuilder sql = new StringBuilder("UPDATE `")
                .append(definition.name)
                .append("` SET ")
                .append(setSql)
                .append(" WHERE ");
        appendKeyWhere(sql, args, definition, data);

        return jdbcTemplate.update(sql.toString(), args.toArray());
    }

    public int delete(String entity, Map<String, String> keys) {
        TableDefinition definition = requireDefinition(entity);
        for (String key : definition.keyColumns) {
            if (keys.get(key) == null || keys.get(key).isBlank()) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "缺少主键字段: " + key);
            }
        }

        StringBuilder sql = new StringBuilder("DELETE FROM `")
                .append(definition.name)
                .append("` WHERE ");
        List<Object> args = new ArrayList<>();
        appendKeyWhere(sql, args, definition, keys);
        return jdbcTemplate.update(sql.toString(), args.toArray());
    }

    private TableDefinition requireDefinition(String entity) {
        TableDefinition definition = definitions.get(entity);
        if (definition == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "未知数据模块: " + entity);
        }
        return definition;
    }

    private List<String> getColumns(String tableName) {
        return columnsCache.computeIfAbsent(tableName, name ->
                jdbcTemplate.query(
                        "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
                        (rs, rowNum) -> rs.getString(1),
                        name
                )
        );
    }

    private Map<String, Object> filterData(Map<String, Object> body, Set<String> allowedColumns) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (body == null) {
            return result;
        }
        body.forEach((key, value) -> {
            if (allowedColumns.contains(key) && value != null) {
                result.put(key, value);
            }
        });
        return result;
    }

    private void appendKeyWhere(
            StringBuilder sql,
            List<Object> args,
            TableDefinition definition,
            Map<String, ?> data
    ) {
        for (int i = 0; i < definition.keyColumns.size(); i++) {
            if (i > 0) {
                sql.append(" AND ");
            }
            String key = definition.keyColumns.get(i);
            sql.append("`").append(key).append("` = ?");
            args.add(data.get(key));
        }
    }

    private void registerDefinitions() {
        definitions.put("customer", new TableDefinition(
                "customer",
                List.of("id"),
                List.of("company", "lxr", "lxrMobileNo", "pinyin"),
                Set.of("type", "status", "diqu", "fenlei")
        ));
        definitions.put("product", new TableDefinition(
                "product",
                List.of("id"),
                List.of("id", "libName", "prd_category"),
                Set.of("prd_category"),
                Set.of("id", "libName")
        ));
        definitions.put("lib", new TableDefinition(
                "lib",
                List.of("productId", "xh"),
                List.of("testItem", "basis", "mathod", "stdValue"),
                Set.of("productId", "basis", "mathod")
        ));
        definitions.put("project", new TableDefinition(
                "project",
                List.of("id_nj"),
                List.of("bh", "customerId", "productName", "taskno", "htbh", "result"),
                Set.of("state_register", "state_div", "state_arrange", "state_examine", "state_verify",
                        "state_isstop", "result", "state_comlete", "date_jycomplete", "date_reg", "date_cy"),
                Set.of("bh", "productName", "dwname_wt_nj", "dwname_sc_nj", "dwname_sj_nj",
                        "taskbatchno", "taskno", "userId_cy", "customerId_sjdw", "scdateflag"),
                Set.of("date_cy")
        ));
        definitions.put("prj_detail", new TableDefinition(
                "prj_detail",
                List.of("project_bh", "item_xh"),
                List.of("testItem", "basis", "mathod", "jyResult", "item_evaluate"),
                Set.of("project_bh", "userId_jyy", "item_evaluate", "jyResult")
        ));
        definitions.put("task", new TableDefinition(
                "task",
                List.of("id"),
                List.of("project_bh", "userId_jyy", "state_jyy"),
                Set.of("project_bh", "userId_jyy", "state_jyy")
        ));
        definitions.put("sjtask", new TableDefinition(
                "sjtask",
                List.of("id"),
                List.of("taskno", "leader", "source", "region", "taskname", "taskxz"),
                Set.of("taskxz", "region", "state", "date"),
                Set.of("taskno", "source", "note")
        ));
        definitions.put("user", new TableDefinition(
                "user",
                List.of("gh"),
                List.of("gh", "name", "mobile", "email", "position"),
                Set.of("gh", "userRoleId", "deptId", "nature", "position")
        ));
        definitions.put("user_method", new TableDefinition(
                "user_method",
                List.of("id"),
                List.of("usergh", "method", "method_note"),
                Set.of("usergh", "method")
        ));
        definitions.put("user_item", new TableDefinition(
                "user_item",
                List.of("id"),
                List.of("usergh", "item"),
                Set.of("usergh", "item")
        ));
        definitions.put("basisname", new TableDefinition(
                "basisname",
                List.of("id"),
                List.of("basis", "basisName"),
                Set.of("basis")
        ));
        definitions.put("contract", new TableDefinition(
                "contract",
                List.of("id"),
                List.of("htbh", "customerId", "content"),
                Set.of("customerId", "alive_status", "isreceive")
        ));
    }

    private static class TableDefinition {
        private final String name;
        private final List<String> keyColumns;
        private final List<String> searchColumns;
        private final Set<String> filterColumns;
        private final Set<String> likeColumns;
        /**
         * 以 varchar 存储的遗留日期列（格式如 2024.8.22 / %Y.%c.%e），
         * 区间比较时用 STR_TO_DATE 转换，避免字符串比较错乱
         */
        private final Set<String> dateVarcharColumns;

        private TableDefinition(
                String name,
                List<String> keyColumns,
                List<String> searchColumns,
                Set<String> filterColumns
        ) {
            this(name, keyColumns, searchColumns, filterColumns, Set.of(), Set.of());
        }

        private TableDefinition(
                String name,
                List<String> keyColumns,
                List<String> searchColumns,
                Set<String> filterColumns,
                Set<String> likeColumns
        ) {
            this(name, keyColumns, searchColumns, filterColumns, likeColumns, Set.of());
        }

        private TableDefinition(
                String name,
                List<String> keyColumns,
                List<String> searchColumns,
                Set<String> filterColumns,
                Set<String> likeColumns,
                Set<String> dateVarcharColumns
        ) {
            this.name = name;
            this.keyColumns = keyColumns;
            this.searchColumns = searchColumns;
            this.filterColumns = filterColumns;
            this.likeColumns = likeColumns;
            this.dateVarcharColumns = dateVarcharColumns;
        }
    }
}
