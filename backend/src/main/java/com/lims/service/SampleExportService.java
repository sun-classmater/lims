package com.lims.service;

import com.lims.common.BusinessException;
import com.lims.common.ResultCode;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 样品管理 - 导出数据。
 *
 * 把页面勾选的样品及其全部检测项结果导出为 Excel（.xls，POI HSSF）：
 * - water / agri / livestock：上报口径，横向一行一检测项，带任务、样品、单位等上报字段；
 * - detail：检验明细口径，一行一检测项，带检验员与完成日期。
 *
 * 说明：旧系统的水检 / 农检 / 畜检上报是三种固定的上报表格模板，
 * 库里没有可用于区分产品大类的字段（project.category 全为空），
 * 因此三种上报目前使用同一套上报字段、仅在标题上标注上报类别；
 * 后续拿到官方模板或分类规则后，只需在这里调整列定义与筛选。
 */
@Service
public class SampleExportService {

    private static final int MAX_SAMPLES = 500;

    private static final String[] REPORT_HEADERS = {
            "序号", "任务编号", "样品编号", "产品名称", "受检单位", "生产单位", "抽样地点",
            "抽样日期", "登记日期", "检验完成日期", "检测项目", "检验依据", "检验方法",
            "指标要求", "检验结果", "单项评价"
    };

    private static final String[] DETAIL_HEADERS = {
            "序号", "样品编号", "产品名称", "检测项目", "检验依据", "检验方法",
            "指标要求", "检验结果", "单项评价", "单项评价说明", "检验员", "检验完成日期"
    };

    private final JdbcTemplate jdbcTemplate;

    public SampleExportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String typeLabel(String type) {
        return switch (normalizeType(type)) {
            case "water" -> "水检";
            case "agri" -> "农检";
            case "livestock" -> "畜检";
            default -> "检验明细";
        };
    }

    public String fileName(String type) {
        return switch (normalizeType(type)) {
            case "water" -> "水检上报数据.xls";
            case "agri" -> "农检上报数据.xls";
            case "livestock" -> "畜检上报数据.xls";
            default -> "检验明细数据.xls";
        };
    }

    public byte[] buildExport(String type, List<String> rawBhs) {
        String exportType = normalizeType(type);
        List<String> bhs = normalizeBhs(rawBhs);
        if (bhs.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请先勾选需要导出的样品");
        }
        if (bhs.size() > MAX_SAMPLES) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "单次最多导出 " + MAX_SAMPLES + " 个样品，请缩小勾选范围");
        }

        List<Map<String, Object>> projects = loadProjects(bhs);
        if (projects.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "未找到勾选的样品记录");
        }
        String placeholders = String.join(",", bhs.stream().map(item -> "?").toList());
        List<Map<String, Object>> details = jdbcTemplate.queryForList(
                "SELECT project_bh, item_xh, testItem, basis, mathod, stdValue, jyResult, "
                        + "item_evaluate, jyResult_note, userId_jyy FROM prj_detail "
                        + "WHERE project_bh IN (" + placeholders + ") ORDER BY project_bh, item_xh",
                bhs.toArray());
        Map<String, List<Map<String, Object>>> detailsByBh = new LinkedHashMap<>();
        for (Map<String, Object> detail : details) {
            detailsByBh
                    .computeIfAbsent(text(detail.get("project_bh")), key -> new ArrayList<>())
                    .add(detail);
        }
        Map<String, String> finishDates = loadFinishDates(bhs);
        Map<String, String> userNames = loadUserNames();

        try (Workbook workbook = new HSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            boolean detailMode = "detail".equals(exportType);
            String[] headers = detailMode ? DETAIL_HEADERS : REPORT_HEADERS;
            String title = detailMode
                    ? "样品检验明细表"
                    : "样品检测结果上报表（" + typeLabel(exportType) + "）";

            Sheet sheet = workbook.createSheet(typeLabel(exportType));
            int rowIndex = writeTitle(sheet, styles, title, headers.length - 1);

            Row headerRow = sheet.createRow(rowIndex++);
            for (int i = 0; i < headers.length; i++) {
                writeCell(headerRow, i, headers[i], styles.header);
            }

            int sequence = 1;
            for (Map<String, Object> project : projects) {
                String bh = text(project.get("bh"));
                List<Map<String, Object>> projectDetails = detailsByBh.getOrDefault(bh, List.of());
                if (projectDetails.isEmpty()) {
                    rowIndex = writeExportRow(sheet, rowIndex, styles, detailMode, sequence++,
                            project, Map.of(), finishDates, userNames);
                    continue;
                }
                for (Map<String, Object> detail : projectDetails) {
                    rowIndex = writeExportRow(sheet, rowIndex, styles, detailMode, sequence++,
                            project, detail, finishDates, userNames);
                }
            }

            rowIndex++;
            Row footerRow = sheet.createRow(rowIndex);
            Cell footerCell = footerRow.createCell(0);
            footerCell.setCellValue("本表由 LIMS 系统按样品登记与检验数据自动生成。");
            footerCell.setCellStyle(styles.footer);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, headers.length - 1));

            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 15 * 256);
            }
            sheet.setColumnWidth(1, 20 * 256);
            sheet.setColumnWidth(2, 20 * 256);
            sheet.setColumnWidth(headers.length - 2, 24 * 256);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "导出失败");
        }
    }

    private int writeExportRow(Sheet sheet, int rowIndex, Styles styles, boolean detailMode,
                               int sequence, Map<String, Object> project,
                               Map<String, Object> detail, Map<String, String> finishDates,
                               Map<String, String> userNames) {
        String bh = text(project.get("bh"));
        String finishDate = firstText(project.get("date_jycomplete"), finishDates.get(bh));
        Row row = sheet.createRow(rowIndex);

        if (detailMode) {
            int column = 0;
            writeCell(row, column++, String.valueOf(sequence), styles.body);
            writeCell(row, column++, bh, styles.body);
            writeCell(row, column++, text(project.get("productName")), styles.body);
            writeCell(row, column++, text(detail.get("testItem")), styles.body);
            writeCell(row, column++, text(detail.get("basis")), styles.body);
            writeCell(row, column++, text(detail.get("mathod")), styles.body);
            writeCell(row, column++, text(detail.get("stdValue")), styles.body);
            writeCell(row, column++, text(detail.get("jyResult")), styles.body);
            writeCell(row, column++, text(detail.get("item_evaluate")), styles.body);
            writeCell(row, column++, text(detail.get("jyResult_note")), styles.body);
            writeCell(row, column++, userName(userNames, detail.get("userId_jyy")), styles.body);
            writeCell(row, column, finishDate, styles.body);
            return rowIndex + 1;
        }

        int column = 0;
        writeCell(row, column++, String.valueOf(sequence), styles.body);
        writeCell(row, column++, text(project.get("taskno")), styles.body);
        writeCell(row, column++, bh, styles.body);
        writeCell(row, column++, text(project.get("productName")), styles.body);
        writeCell(row, column++, text(project.get("dwname_sj_nj")), styles.body);
        writeCell(row, column++, text(project.get("dwname_sc_nj")), styles.body);
        writeCell(row, column++, text(project.get("cyaddr")), styles.body);
        writeCell(row, column++, text(project.get("date_cy")), styles.body);
        writeCell(row, column++, text(project.get("date_reg")), styles.body);
        writeCell(row, column++, finishDate, styles.body);
        writeCell(row, column++, text(detail.get("testItem")), styles.body);
        writeCell(row, column++, text(detail.get("basis")), styles.body);
        writeCell(row, column++, text(detail.get("mathod")), styles.body);
        writeCell(row, column++, text(detail.get("stdValue")), styles.body);
        writeCell(row, column++, text(detail.get("jyResult")), styles.body);
        writeCell(row, column, text(detail.get("item_evaluate")), styles.body);
        return rowIndex + 1;
    }

    private int writeTitle(Sheet sheet, Styles styles, String title, int lastColumn) {
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(26);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, lastColumn));
        return 2;
    }

    private String normalizeType(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "缺少导出类型");
        }
        return switch (type.trim().toLowerCase()) {
            case "water", "agri", "livestock", "detail" -> type.trim().toLowerCase();
            default -> throw new BusinessException(
                    ResultCode.BAD_REQUEST.getCode(), "未知的导出类型: " + type);
        };
    }

    private static List<String> normalizeBhs(List<String> rawBhs) {
        List<String> bhs = new ArrayList<>();
        if (rawBhs == null) {
            return bhs;
        }
        for (String raw : rawBhs) {
            String bh = text(raw);
            if (!bh.isEmpty() && !bhs.contains(bh)) {
                bhs.add(bh);
            }
        }
        return bhs;
    }

    private List<Map<String, Object>> loadProjects(List<String> bhs) {
        String placeholders = String.join(",", bhs.stream().map(item -> "?").toList());
        return jdbcTemplate.queryForList(
                "SELECT * FROM project WHERE TRIM(bh) IN (" + placeholders + ")",
                bhs.toArray());
    }

    private Map<String, String> loadFinishDates(List<String> bhs) {
        Map<String, String> finishDates = new HashMap<>();
        String placeholders = String.join(",", bhs.stream().map(item -> "?").toList());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT project_bh, date_jyy FROM task WHERE project_bh IN ("
                        + placeholders + ") ORDER BY id",
                bhs.toArray());
        for (Map<String, Object> row : rows) {
            String bh = text(row.get("project_bh"));
            String date = text(row.get("date_jyy"));
            if (!date.isEmpty()) {
                finishDates.putIfAbsent(bh, date);
            }
        }
        return finishDates;
    }

    private Map<String, String> loadUserNames() {
        Map<String, String> names = new LinkedHashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT gh, name FROM `user`");
        for (Map<String, Object> row : rows) {
            names.put(text(row.get("gh")), text(row.get("name")));
        }
        return names;
    }

    private static String userName(Map<String, String> names, Object gh) {
        String key = text(gh);
        if (key.isEmpty()) {
            return "";
        }
        String name = names.get(key);
        return name == null || name.isEmpty() ? key : name;
    }

    private static void writeCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String firstText(Object primary, String fallback) {
        String value = text(primary);
        if (!value.isEmpty()) {
            return value;
        }
        return fallback == null ? "" : fallback;
    }

    private static final class Styles {

        private final CellStyle title;
        private final CellStyle header;
        private final CellStyle body;
        private final CellStyle footer;

        private Styles(Workbook workbook) {
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            title = workbook.createCellStyle();
            title.setFont(titleFont);
            title.setAlignment(HorizontalAlignment.CENTER);
            title.setVerticalAlignment(VerticalAlignment.CENTER);

            header = workbook.createCellStyle();
            header.setFont(boldFont);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorder(header);

            body = workbook.createCellStyle();
            body.setVerticalAlignment(VerticalAlignment.CENTER);
            body.setWrapText(true);
            applyBorder(body);

            footer = workbook.createCellStyle();
            footer.setVerticalAlignment(VerticalAlignment.CENTER);
        }

        private static void applyBorder(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        }
    }
}
