package com.lims.service;

import com.lims.common.BusinessException;
import com.lims.common.ResultCode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 样品管理 - 报告下载。
 *
 * 以旧系统的两份 Word 报告为模板：
 * - CMA_CATL：带资质认定号 [2020]农质检核（苏）字第0021号；
 * - CMA：不带该行。
 * 按样品编号替换封面与正文中的字段，并按检验明细重建结果表。
 * 下载文件名为「样品编号-受检单位.doc」。
 */
@Service
public class ReportService {

    private static final Charset GBK = Charset.forName("GBK");
    private static final Pattern ROW_PATTERN =
            Pattern.compile("<w:tr\\b[^>]*>.*?</w:tr>", Pattern.DOTALL);
    private static final Pattern CELL_PATTERN =
            Pattern.compile("<w:tc\\b[^>]*>.*?</w:tc>", Pattern.DOTALL);
    private static final Pattern TEXT_PATTERN =
            Pattern.compile("<w:t(\\s[^>]*)?>(.*?)</w:t>", Pattern.DOTALL);

    private static final String TEMPLATE_CONCLUSION =
            "本样品所检项目按农业部公告第783号-1-2006、农业部公告第1077号-1-2008、"
                    + "农业部公告第1077号-1-2012、GB/T19857-2005、GB/T20756-2006规定的要求进行判定，"
                    + "其中恩诺沙星和环丙沙星超标，判定为不合格。";
    private static final String TEMPLATE_BASIS =
            "农业部公告第783号-1-2006、农业部公告第1077号-1-2008、"
                    + "农业部公告第1077号-1-2012、GB/T19857-2005、GB/T20756-2006";
    private static final DateTimeFormatter CN_DATE = DateTimeFormatter.ofPattern("yyyy年M月d日");

    private final JdbcTemplate jdbcTemplate;

    public ReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 报告类型：cma（CMA 报告）、catl（CMA_CATL 报告） */
    public String normalizeType(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "缺少报告类型");
        }
        return switch (type.trim().toLowerCase()) {
            case "cma" -> "CMA";
            case "catl" -> "CMA_CATL";
            default -> throw new BusinessException(
                    ResultCode.BAD_REQUEST.getCode(), "未知的报告类型: " + type);
        };
    }

    /** 下载文件名：样品编号-受检单位.doc */
    public String fileName(String type, String bh) {
        normalizeType(type);
        Map<String, Object> project = loadProject(bh);
        if (project == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(),
                    "未找到样品编号为 " + text(bh) + " 的项目记录");
        }
        return text(bh) + "-" + unitOf(project) + ".doc";
    }

    public byte[] buildReport(String type, String bh) {
        String reportType = normalizeType(type);
        String sampleBh = text(bh);
        if (sampleBh.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "缺少样品编号");
        }

        Map<String, Object> project = loadProject(sampleBh);
        if (project == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(),
                    "未找到样品编号为 " + sampleBh + " 的项目记录");
        }
        List<Map<String, Object>> details = jdbcTemplate.queryForList(
                "SELECT item_xh, testItem, basis, stdValue, num_lowerst, jyResult, item_evaluate "
                        + "FROM prj_detail WHERE project_bh = ? ORDER BY item_xh",
                sampleBh
        );

        String xml = readTemplate("CMA_CATL".equals(reportType)
                ? "templates/report-catl.doc"
                : "templates/report-cma.doc");

        String basis = basisList(details);
        String conclusion = conclusionText(basis, details);

        xml = xml.replace(TEMPLATE_CONCLUSION, esc(conclusion));
        xml = xml.replace("JK(2023)-SA-182", esc(sampleBh));
        xml = xml.replace("鲢鱼", esc(text(project.get("productName"))));
        xml = xml.replace("个体户", esc(unitOf(project)));
        xml = xml.replace("专项监督", esc(text(project.get("jylb"))));
        xml = xml.replace("如东县袁庄镇戴南村2组", esc(text(project.get("cyaddr"))));
        xml = xml.replace("2023.12.13", esc(text(project.get("date_cy"))));
        xml = xml.replace("4kg", esc(text(project.get("ypNum"))));
        xml = xml.replace("徐汇宏", esc(samplerName(project)));
        xml = xml.replace("2023年12月26日~2023年12月26日",
                esc(dateRange(project.get("date_jystart"), project.get("date_jycomplete"))));
        xml = xml.replace("2026年10月20日", esc(LocalDate.now().format(CN_DATE)));
        xml = xml.replace(TEMPLATE_BASIS, esc(basis));
        xml = rebuildTable(xml, details);

        return xml.getBytes(GBK);
    }

    /** 用检验明细重建结果表：保留表头与“以下空白”行，按模板行格式生成数据行 */
    private String rebuildTable(String xml, List<Map<String, Object>> details) {
        int anchor = xml.indexOf("单项结论");
        if (anchor < 0) {
            return xml;
        }
        int tableStart = xml.lastIndexOf("<w:tbl", anchor);
        int tableEnd = xml.indexOf("</w:tbl>", anchor);
        if (tableStart < 0 || tableEnd < 0) {
            return xml;
        }
        tableEnd += "</w:tbl>".length();
        String table = xml.substring(tableStart, tableEnd);

        List<String> rows = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();
        Matcher rowMatcher = ROW_PATTERN.matcher(table);
        while (rowMatcher.find()) {
            rows.add(rowMatcher.group());
            starts.add(rowMatcher.start());
            ends.add(rowMatcher.end());
        }
        if (rows.size() < 3) {
            return xml;
        }

        int tailIndex = -1;
        for (int i = rows.size() - 1; i >= 1; i--) {
            if (rows.get(i).contains("以下空白")) {
                tailIndex = i;
                break;
            }
        }
        if (tailIndex < 0) {
            tailIndex = rows.size() - 1;
        }

        String headerRow = rows.get(0);
        String dataTemplate = rows.get(1);
        String tailRow = rows.get(tailIndex);

        StringBuilder sb = new StringBuilder();
        sb.append(table, 0, starts.get(0));
        sb.append(headerRow);
        for (Map<String, Object> item : details) {
            sb.append(fillRow(dataTemplate, rowValues(item)));
        }
        sb.append(tailRow);
        sb.append(table.substring(ends.get(rows.size() - 1)));

        return xml.substring(0, tableStart) + sb + xml.substring(tableEnd);
    }

    /** 按单元格顺序替换一行中的 7 个文本节点 */
    private String fillRow(String row, String[] values) {
        Matcher cellMatcher = CELL_PATTERN.matcher(row);
        StringBuffer sb = new StringBuffer();
        int index = 0;
        while (cellMatcher.find()) {
            String cell = cellMatcher.group();
            if (index < values.length) {
                Matcher textMatcher = TEXT_PATTERN.matcher(cell);
                if (textMatcher.find()) {
                    String attributes = textMatcher.group(1) == null ? "" : textMatcher.group(1);
                    String replacement = "<w:t" + attributes + ">" + esc(values[index]) + "</w:t>";
                    cell = cell.substring(0, textMatcher.start())
                            + replacement
                            + cell.substring(textMatcher.end());
                }
            }
            cellMatcher.appendReplacement(sb, Matcher.quoteReplacement(cell));
            index++;
        }
        cellMatcher.appendTail(sb);
        return sb.toString();
    }

    private String[] rowValues(Map<String, Object> item) {
        String testItem = text(item.get("testItem"));
        String name = testItem;
        String unit = "";
        int split = firstSeparator(testItem);
        if (split > 0) {
            name = testItem.substring(0, split).trim();
            unit = testItem.substring(split + 1).trim();
        }
        return new String[]{
                name,
                text(item.get("jyResult")),
                text(item.get("basis")),
                text(item.get("stdValue")),
                unit,
                text(item.get("num_lowerst")),
                text(item.get("item_evaluate"))
        };
    }

    private String conclusionText(String basis, List<Map<String, Object>> details) {
        if (details.isEmpty()) {
            return "本样品暂无检验数据。";
        }
        List<String> failed = new ArrayList<>();
        for (Map<String, Object> item : details) {
            if ("不合格".equals(text(item.get("item_evaluate")))) {
                String name = text(item.get("testItem"));
                int split = firstSeparator(name);
                failed.add(split > 0 ? name.substring(0, split).trim() : name);
            }
        }
        String prefix = basis.isEmpty()
                ? "本样品所检项目"
                : "本样品所检项目按" + basis + "规定的要求进行判定，";
        if (failed.isEmpty()) {
            return prefix + "所检项目均符合要求，判定为合格。";
        }
        return prefix + "其中" + String.join("、", failed) + "超标，判定为不合格。";
    }

    private String basisList(List<Map<String, Object>> details) {
        Set<String> values = new LinkedHashSet<>();
        for (Map<String, Object> item : details) {
            String basis = text(item.get("basis"));
            if (!basis.isEmpty()) {
                values.add(basis);
            }
        }
        return String.join("、", values);
    }

    private Map<String, Object> loadProject(String bh) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM project WHERE TRIM(bh) = ? ORDER BY id_nj DESC LIMIT 1",
                text(bh)
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 受检单位：优先客户名称，其次样品上的受检单位、委托单位 */
    private String unitOf(Map<String, Object> project) {
        String customerId = text(project.get("customerId"));
        if (!customerId.isEmpty()) {
            List<String> names = jdbcTemplate.queryForList(
                    "SELECT company FROM customer WHERE id = ? LIMIT 1",
                    String.class,
                    customerId
            );
            if (!names.isEmpty() && !text(names.get(0)).isEmpty()) {
                return text(names.get(0));
            }
        }
        String inspected = text(project.get("dwname_sj_nj"));
        return inspected.isEmpty() ? text(project.get("dwname_wt_nj")) : inspected;
    }

    private String samplerName(Map<String, Object> project) {
        String gh = text(project.get("userId_cy"));
        if (gh.isEmpty()) {
            return "";
        }
        List<String> names = jdbcTemplate.queryForList(
                "SELECT name FROM `user` WHERE gh = ? LIMIT 1",
                String.class,
                gh
        );
        return names.isEmpty() ? gh : text(names.get(0));
    }

    private String dateRange(Object start, Object end) {
        String from = cnDate(text(start));
        String to = cnDate(text(end));
        if (from.isEmpty()) {
            from = to;
        }
        if (to.isEmpty()) {
            to = from;
        }
        if (from.isEmpty()) {
            return "";
        }
        return from + "~" + to;
    }

    private String cnDate(String raw) {
        if (raw.isEmpty()) {
            return "";
        }
        String normalized = raw.replace('.', '-').replace('/', '-');
        String[] parts = normalized.split("-");
        if (parts.length == 3) {
            try {
                return LocalDate.of(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2])
                ).format(CN_DATE);
            } catch (NumberFormatException | java.time.DateTimeException ignored) {
                return raw;
            }
        }
        return raw;
    }

    private String readTemplate(String path) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return new String(input.readAllBytes(), GBK);
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "报告模板读取失败");
        }
    }

    private static int firstSeparator(String value) {
        int comma = value.indexOf(',');
        int cnComma = value.indexOf('，');
        if (comma < 0) {
            return cnComma;
        }
        if (cnComma < 0) {
            return comma;
        }
        return Math.min(comma, cnComma);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String esc(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
