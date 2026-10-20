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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 样品管理 - 报告下载。
 *
 * 按样品编号（bh）把 project 表的登记信息与 prj_detail 的检验明细
 * 汇总成一份 Excel（.xls，POI HSSF）报告，供页面下载留存。
 *
 * 说明：旧系统的 CMA / CMA_CATL 报告是固定排版的正式报告模板，
 * 本服务生成的是包含同等数据的系统报告；后续若有正式模板，
 * 只需替换 buildReport 的排版部分，取数逻辑可复用。
 */
@Service
public class ReportService {

    private static final String[] DETAIL_HEADERS = {
            "序号", "检测项目", "检验依据", "检验方法", "指标要求",
            "检验结果", "单项评价", "单项评价说明", "检验员", "完成日期"
    };

    private static final int LAST_COLUMN = DETAIL_HEADERS.length - 1;

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

    /** 下载文件名，如 JK(2023)-SA-422-CMA报告.xls */
    public String fileName(String type, String bh) {
        String sampleBh = bh == null ? "" : bh.trim();
        return sampleBh + "-" + normalizeType(type) + "报告.xls";
    }

    public byte[] buildReport(String type, String bh) {
        String reportType = normalizeType(type);
        String sampleBh = bh == null ? "" : bh.trim();
        if (sampleBh.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "缺少样品编号");
        }

        Map<String, Object> project = loadProject(sampleBh);
        if (project == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(),
                    "未找到样品编号为 " + sampleBh + " 的项目记录");
        }

        List<Map<String, Object>> details = jdbcTemplate.queryForList(
                "SELECT item_xh, testItem, basis, mathod, stdValue, jyResult, item_evaluate, "
                        + "jyResult_note, userId_jyy FROM prj_detail "
                        + "WHERE project_bh = ? ORDER BY item_xh",
                sampleBh);
        Map<String, String> userNames = loadUserNames();
        String finishDate = firstText(project.get("date_jycomplete"), loadTaskFinishDate(sampleBh));

        try (Workbook workbook = new HSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("检测报告");
            Styles styles = new Styles(workbook);
            int rowIndex = 0;

            Row titleRow = sheet.createRow(rowIndex++);
            titleRow.setHeightInPoints(28);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("检验检测报告（" + reportType + "）");
            titleCell.setCellStyle(styles.title);
            merge(sheet, 0, 0);

            Row sampleRow = sheet.createRow(rowIndex++);
            Cell sampleCell = sampleRow.createCell(0);
            sampleCell.setCellValue("样品编号：" + sampleBh);
            sampleCell.setCellStyle(styles.subTitle);
            merge(sheet, 1, 1);

            rowIndex++;

            rowIndex = writeInfoRow(sheet, rowIndex, styles,
                    "样品编号", sampleBh, "任务编号", text(project.get("taskno")));
            rowIndex = writeInfoRow(sheet, rowIndex, styles,
                    "受检单位", text(project.get("dwname_sj_nj")),
                    "委托单位", text(project.get("dwname_wt_nj")));
            rowIndex = writeInfoRow(sheet, rowIndex, styles,
                    "生产单位", text(project.get("dwname_sc_nj")),
                    "产品名称", text(project.get("productName")));
            rowIndex = writeInfoRow(sheet, rowIndex, styles,
                    "规格型号", text(project.get("ggxh")), "样品等级", text(project.get("ypdj")));
            rowIndex = writeInfoRow(sheet, rowIndex, styles,
                    "样品数量", text(project.get("ypNum")), "抽样地点", text(project.get("cyaddr")));
            rowIndex = writeInfoRow(sheet, rowIndex, styles,
                    "抽样日期", text(project.get("date_cy")), "登记日期", text(project.get("date_reg")));
            rowIndex = writeInfoRow(sheet, rowIndex, styles,
                    "检验完成日期", finishDate, "检验结论", text(project.get("result")));
            rowIndex = writeInfoRow(sheet, rowIndex, styles,
                    "项目登记备注", text(project.get("remarks_register")),
                    "项目分解备注", text(project.get("remarks_div")));

            rowIndex++;

            Row detailTitleRow = sheet.createRow(rowIndex);
            Cell detailTitleCell = detailTitleRow.createCell(0);
            detailTitleCell.setCellValue("检验项目明细");
            detailTitleCell.setCellStyle(styles.subTitle);
            merge(sheet, rowIndex, rowIndex);
            rowIndex++;

            Row headerRow = sheet.createRow(rowIndex++);
            for (int i = 0; i < DETAIL_HEADERS.length; i++) {
                writeCell(headerRow, i, DETAIL_HEADERS[i], styles.header);
            }

            if (details.isEmpty()) {
                Row emptyRow = sheet.createRow(rowIndex);
                Cell emptyCell = emptyRow.createCell(0);
                emptyCell.setCellValue("该项目暂无分解的检验项目");
                emptyCell.setCellStyle(styles.body);
                merge(sheet, rowIndex, rowIndex);
                rowIndex++;
            } else {
                int sequence = 1;
                for (Map<String, Object> detail : details) {
                    Row row = sheet.createRow(rowIndex++);
                    writeCell(row, 0, String.valueOf(sequence++), styles.body);
                    writeCell(row, 1, text(detail.get("testItem")), styles.body);
                    writeCell(row, 2, text(detail.get("basis")), styles.body);
                    writeCell(row, 3, text(detail.get("mathod")), styles.body);
                    writeCell(row, 4, text(detail.get("stdValue")), styles.body);
                    writeCell(row, 5, text(detail.get("jyResult")), styles.body);
                    writeCell(row, 6, text(detail.get("item_evaluate")), styles.body);
                    writeCell(row, 7, text(detail.get("jyResult_note")), styles.body);
                    writeCell(row, 8, userName(userNames, detail.get("userId_jyy")), styles.body);
                    writeCell(row, 9, finishDate, styles.body);
                }
            }

            rowIndex++;
            Row footerRow = sheet.createRow(rowIndex);
            Cell footerCell = footerRow.createCell(0);
            footerCell.setCellValue("本报告由 LIMS 系统按项目登记与检验数据自动生成。");
            footerCell.setCellStyle(styles.footer);
            merge(sheet, rowIndex, rowIndex);

            for (int i = 0; i <= LAST_COLUMN; i++) {
                sheet.setColumnWidth(i, 16 * 256);
            }
            sheet.setColumnWidth(1, 26 * 256);
            sheet.setColumnWidth(3, 30 * 256);
            sheet.setColumnWidth(7, 22 * 256);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "报告生成失败");
        }
    }

    private void merge(Sheet sheet, int firstRow, int lastRow) {
        sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, 0, LAST_COLUMN));
    }

    /** 一行写两组「标签 + 值」，值跨列以便阅读 */
    private int writeInfoRow(Sheet sheet, int rowIndex, Styles styles,
                             String label1, String value1,
                             String label2, String value2) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(19);
        writeCell(row, 0, label1, styles.label);
        writeCell(row, 1, value1, styles.value);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 1, 4));
        writeCell(row, 5, label2, styles.label);
        writeCell(row, 6, value2, styles.value);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 6, LAST_COLUMN));
        return rowIndex + 1;
    }

    private static void writeCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private Map<String, Object> loadProject(String bh) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM project WHERE TRIM(bh) = ? ORDER BY id_nj DESC LIMIT 1", bh);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String loadTaskFinishDate(String bh) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT date_jyy FROM task WHERE project_bh = ? ORDER BY id", bh);
        for (Map<String, Object> row : rows) {
            String value = text(row.get("date_jyy"));
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
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

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String firstText(Object primary, String fallback) {
        String value = text(primary);
        return value.isEmpty() ? fallback : value;
    }

    private static final class Styles {

        private final CellStyle title;
        private final CellStyle subTitle;
        private final CellStyle label;
        private final CellStyle value;
        private final CellStyle header;
        private final CellStyle body;
        private final CellStyle footer;

        private Styles(Workbook workbook) {
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            title = workbook.createCellStyle();
            title.setFont(titleFont);
            title.setAlignment(HorizontalAlignment.CENTER);
            title.setVerticalAlignment(VerticalAlignment.CENTER);

            subTitle = workbook.createCellStyle();
            subTitle.setFont(boldFont);
            subTitle.setVerticalAlignment(VerticalAlignment.CENTER);

            label = workbook.createCellStyle();
            label.setAlignment(HorizontalAlignment.RIGHT);
            label.setVerticalAlignment(VerticalAlignment.CENTER);

            value = workbook.createCellStyle();
            value.setAlignment(HorizontalAlignment.LEFT);
            value.setVerticalAlignment(VerticalAlignment.CENTER);

            header = workbook.createCellStyle();
            header.setFont(boldFont);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorder(header);

            body = workbook.createCellStyle();
            body.setAlignment(HorizontalAlignment.LEFT);
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
