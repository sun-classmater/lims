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
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 基础数据 - 项目库维护：导入检验项目库（lib.xls）。
 *
 * 文件格式（与旧系统一致）：
 * 1. 扩展名 .xls，数据位于名为 sheet1 的工作表；
 * 2. A1=项目库产品编号，B1=产品分类名称，C1=检验项目表名称；
 * 3. 第 2 行为列标题：序号 / 检测项目 / 检验依据 / 检验方法 / 第几法 / 标准值 / 检出限；
 * 4. 检验项目数据从第 3 行开始。
 *
 * 上传后先解析预览，用户确认无误再写入 product 与 lib 表。
 */
@Service
public class LibImportService {

    private static final String SHEET_NAME = "sheet1";
    private static final String TEMP_DIR = "lib-import-tmp";
    private static final List<String> TEMPLATE_HEADERS = List.of(
            "序号", "检测项目", "检验依据", "检验方法", "第几法", "标准值", "检出限"
    );

    private final JdbcTemplate jdbcTemplate;

    @Value("${lims.upload-dir:./uploads}")
    private String uploadDir;

    public LibImportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 上传并解析，返回预览数据 */
    public Map<String, Object> preview(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择需要导入的项目库文件");
        }
        String originalName = file.getOriginalFilename() == null ? "lib.xls" : file.getOriginalFilename();
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件读取失败");
        }

        ParsedLib parsed = parse(content, originalName);
        String token = UUID.randomUUID().toString();
        try {
            Path dir = Paths.get(uploadDir, TEMP_DIR).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Files.write(dir.resolve(token + ".xls"), content);
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "文件暂存失败");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("productId", parsed.productId);
        result.put("prdCategory", parsed.prdCategory);
        result.put("libName", parsed.libName);
        result.put("total", parsed.rows.size());
        result.put("duplicated", productExists(parsed.productId));
        result.put("rows", parsed.rows.subList(0, Math.min(parsed.rows.size(), 100)));
        return result;
    }

    /** 确认导入：写入 product 与 lib 表 */
    @Transactional
    public Map<String, Object> confirm(String token) {
        String value = token == null ? "" : token.trim();
        if (value.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "缺少导入令牌，请重新上传文件");
        }
        Path file = Paths.get(uploadDir, TEMP_DIR, value + ".xls").toAbsolutePath().normalize();
        if (!Files.exists(file)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "导入文件已失效，请重新上传");
        }

        byte[] content;
        try {
            content = Files.readAllBytes(file);
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "导入文件读取失败");
        }
        ParsedLib parsed = parse(content, "lib.xls");
        if (productExists(parsed.productId)) {
            throw new BusinessException(
                    ResultCode.BAD_REQUEST.getCode(),
                    "项目编号 " + parsed.productId + " 已存在，请先删除该编号项目再导入"
            );
        }

        jdbcTemplate.update(
                "INSERT INTO product (id, prd_category, libName) VALUES (?, ?, ?)",
                parsed.productId, parsed.prdCategory, parsed.libName
        );
        List<Object[]> args = new ArrayList<>();
        for (LibRow row : parsed.rows) {
            args.add(new Object[]{
                    parsed.productId, row.xh, row.testItem, row.basis,
                    row.mathod, row.standardValue, row.lowerLimit, row.methodNote
            });
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO lib (productId, xh, testItem, basis, mathod, stdValue, num_lowerst, method_note) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                args
        );

        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // 临时文件删除失败不影响导入结果
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productId", parsed.productId);
        result.put("libName", parsed.libName);
        result.put("inserted", parsed.rows.size());
        return result;
    }

    /** 下载项目库导入模板 */
    public byte[] buildTemplate() {
        try (Workbook workbook = new HSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle bodyStyle = bodyStyle(workbook);

            Row metaRow = sheet.createRow(0);
            String[] meta = {"SA001", "一、调味品", "SA表sa001-1调味品产品检验项目"};
            for (int i = 0; i < meta.length; i++) {
                Cell cell = metaRow.createCell(i);
                cell.setCellValue(meta[i]);
                cell.setCellStyle(bodyStyle);
            }

            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < TEMPLATE_HEADERS.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(TEMPLATE_HEADERS.get(i));
                cell.setCellStyle(headerStyle);
            }

            String[][] examples = {
                    {"1", "苯甲酸及其钠盐（以苯甲酸计），g/kg", "GB 2760-2014", "GB 5009.28-2016", "第一法", "≤1.0", "0.9"},
                    {"2", "山梨酸及其钾盐（以山梨酸计），g/kg", "GB 2760-2014", "GB 5009.28-2016", "第一法", "≤1.0", "0.9"},
                    {"3", "脱氢乙酸及其钠盐（以脱氢乙酸计），g/kg", "GB 2760-2014", "GB 5009.121-2016", "第二法", "≤0.5", "0.4"}
            };
            for (int r = 0; r < examples.length; r++) {
                Row row = sheet.createRow(r + 2);
                for (int c = 0; c < examples[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(examples[r][c]);
                    cell.setCellStyle(bodyStyle);
                }
            }

            for (int i = 0; i < TEMPLATE_HEADERS.size(); i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }
            sheet.setColumnWidth(1, 42 * 256);
            sheet.setColumnWidth(2, 22 * 256);
            sheet.setColumnWidth(3, 24 * 256);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "模板生成失败");
        }
    }

    private ParsedLib parse(byte[] content, String originalName) {
        if (originalName == null || !originalName.toLowerCase().endsWith(".xls")) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件扩展名必须为 .xls");
        }
        try (Workbook workbook = new HSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                throw new BusinessException(
                        ResultCode.BAD_REQUEST.getCode(),
                        "数据必须位于名为 \"" + SHEET_NAME + "\" 的工作表"
                );
            }
            Row metaRow = sheet.getRow(0);
            String productId = cellText(metaRow, 0);
            String prdCategory = cellText(metaRow, 1);
            String libName = cellText(metaRow, 2);
            if (productId.isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "单元格 A1 必须填写项目库产品编号");
            }
            if (prdCategory.isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "单元格 B1 必须填写产品分类名称");
            }
            if (libName.isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "单元格 C1 必须填写检验项目表名称");
            }
            Row headerRow = sheet.getRow(1);
            if (headerRow == null || cellText(headerRow, 1).isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "第 2 行必须为列标题行");
            }

            List<LibRow> rows = new ArrayList<>();
            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String testItem = cellText(row, 1);
                if (testItem.isEmpty()) {
                    continue;
                }
                rows.add(new LibRow(
                        parseInt(cellText(row, 0), rows.size() + 1),
                        testItem,
                        cellText(row, 2),
                        cellText(row, 3),
                        cellText(row, 5),
                        cellText(row, 6),
                        cellText(row, 4)
                ));
            }
            if (rows.isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "第 3 行起没有检测项目数据");
            }
            return new ParsedLib(productId, prdCategory, libName, rows);
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件解析失败，请确认是有效的 .xls 文件");
        }
    }

    private boolean productExists(String productId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product WHERE id = ?",
                Integer.class,
                productId
        );
        return count != null && count > 0;
    }

    private static String cellText(Row row, int index) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double value = cell.getNumericCellValue();
                yield value == Math.rint(value)
                        ? String.valueOf((long) value)
                        : String.valueOf(value);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        applyBorder(style);
        return style;
    }

    private static CellStyle bodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        applyBorder(style);
        return style;
    }

    private static void applyBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private record ParsedLib(String productId, String prdCategory, String libName, List<LibRow> rows) {
    }

    private record LibRow(
            int xh,
            String testItem,
            String basis,
            String mathod,
            String standardValue,
            String lowerLimit,
            String methodNote
    ) {
    }
}
