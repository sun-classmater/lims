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
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务信息 - 中心文件资料。
 *
 * 提供采样信息导入模板与项目库模板的下载：
 * - sample：复用抽样单导入的模板（sheet1 + 文件标识 + 中文表头 + 示例行）；
 * - lib：按 lib 表结构生成的检验项目库模板。
 */
@Service
public class FileLibraryService {

    private static final String[] LIB_HEADERS = {
            "项目库编号", "序号", "检验项目", "检验依据", "检验方法", "指标要求", "检出限", "方法备注"
    };

    private static final String[] LIB_EXAMPLE = {
            "nagw001", "1", "阿维菌素", "GB 2763-2021", "GB 23200.20", "0.02", "0.01", "示例行，导入前请删除"
    };

    private final SampleSheetImportService sampleSheetImportService;

    public FileLibraryService(SampleSheetImportService sampleSheetImportService) {
        this.sampleSheetImportService = sampleSheetImportService;
    }

    public List<Map<String, Object>> documents() {
        return List.of(
                document("sample", "【0】采样信息【导入模板】.xls"),
                document("lib", "项目库模板lib.xls")
        );
    }

    public byte[] build(String type) {
        if ("sample".equals(type)) {
            return sampleSheetImportService.buildTemplate();
        }
        if ("lib".equals(type)) {
            return buildLibTemplate();
        }
        throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "未知的模板类型: " + type);
    }

    public String fileName(String type) {
        if ("sample".equals(type)) {
            return "【0】采样信息【导入模板】.xls";
        }
        if ("lib".equals(type)) {
            return "项目库模板lib.xls";
        }
        throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "未知的模板类型: " + type);
    }

    private byte[] buildLibTemplate() {
        try (Workbook workbook = new HSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("lib");
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle bodyStyle = bodyStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < LIB_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(LIB_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }
            Row exampleRow = sheet.createRow(1);
            for (int i = 0; i < LIB_EXAMPLE.length; i++) {
                Cell cell = exampleRow.createCell(i);
                cell.setCellValue(LIB_EXAMPLE[i]);
                cell.setCellStyle(bodyStyle);
            }
            for (int i = 0; i < LIB_HEADERS.length; i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }
            sheet.setColumnWidth(2, 26 * 256);
            sheet.setColumnWidth(7, 30 * 256);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "模板生成失败");
        }
    }

    private Map<String, Object> document(String type, String name) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", type);
        item.put("name", name);
        return item;
    }

    private static CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorder(style);
        return style;
    }

    private static CellStyle bodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorder(style);
        return style;
    }

    private static void applyBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
