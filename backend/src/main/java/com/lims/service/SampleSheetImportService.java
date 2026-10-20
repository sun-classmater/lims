package com.lims.service;

import com.lims.common.BusinessException;
import com.lims.common.ResultCode;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 抽样单（.xls）上传预览 → 用户确认 → 导入 sjtask。
 *
 * 文件格式要求（与旧 DEMO 一致）：
 * 1. 文件名后缀 .xls，数据位于名为 sheet1 的工作表；
 * 2. A1 为文件标识，用于防止同一文件重复导入；
 * 3. 第 2 行为中文列标题，第 3 行起为数据，按列标题与 sjtask 字段对应。
 */
@Service
public class SampleSheetImportService {

    private static final String SHEET_NAME = "sheet1";
    private static final String TEMP_DIR = "sample-sheet-tmp";

    /**
     * 模板列标题（同时作为第 2 行表头输出），顺序即模板列顺序。
     */
    private static final List<String> TEMPLATE_HEADERS = List.of(
            "任务编号", "责任人", "任务来源", "省市区县", "任务描述", "任务接受日期",
            "任务名称", "任务性质", "任务下达日期", "任务完成日期", "抽样环节", "检测项目"
    );

    /**
     * 中文表头别名 → sjtask 表列名
     */
    private static final Map<String, String> HEADER_ALIASES = Map.ofEntries(
            Map.entry("任务编号", "taskno"),
            Map.entry("任务号", "taskno"),
            Map.entry("责任人", "leader"),
            Map.entry("负责人", "leader"),
            Map.entry("任务来源", "source"),
            Map.entry("来源", "source"),
            Map.entry("省市区县", "region"),
            Map.entry("区域", "region"),
            Map.entry("任务描述", "note"),
            Map.entry("描述", "note"),
            Map.entry("备注", "note"),
            Map.entry("任务接受日期", "date"),
            Map.entry("接受日期", "date"),
            Map.entry("任务名称", "taskname"),
            Map.entry("任务性质", "taskxz"),
            Map.entry("性质", "taskxz"),
            Map.entry("任务下达日期", "xiaddate"),
            Map.entry("下达日期", "xiaddate"),
            Map.entry("任务完成日期", "wancdate"),
            Map.entry("完成日期", "wancdate"),
            Map.entry("抽样环节", "chouyhj"),
            Map.entry("检测项目", "jcxm")
    );

    /**
     * 预览返回中每一列的展示名：sjtask 列名 → 中文标题
     */
    private static final Map<String, String> COLUMN_LABELS = Map.ofEntries(
            Map.entry("taskno", "任务编号"),
            Map.entry("leader", "责任人"),
            Map.entry("source", "任务来源"),
            Map.entry("region", "省市区县"),
            Map.entry("note", "任务描述"),
            Map.entry("date", "任务接受日期"),
            Map.entry("taskname", "任务名称"),
            Map.entry("taskxz", "任务性质"),
            Map.entry("xiaddate", "任务下达日期"),
            Map.entry("wancdate", "任务完成日期"),
            Map.entry("chouyhj", "抽样环节"),
            Map.entry("jcxm", "检测项目")
    );

    private final JdbcTemplate jdbcTemplate;
    private final DataFormatter dataFormatter = new DataFormatter();

    /**
     * token → 待确认的临时文件（预览后 10 分钟内可确认导入）
     */
    private final Map<String, PendingFile> pendingFiles = new java.util.concurrent.ConcurrentHashMap<>();

    @Value("${lims.upload-dir:./uploads}")
    private String uploadDir;

    public SampleSheetImportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PreviewResult preview(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择要上传的 .xls 文件");
        }
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if (!originalName.toLowerCase().endsWith(".xls")) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅支持 .xls 格式的抽样单文件");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件读取失败，请确认是有效的 Excel .xls 文件");
        }

        ParsedSheet parsed = parse(new ByteArrayInputStream(bytes));
        boolean duplicated = markerExists(parsed.marker());

        String token = UUID.randomUUID().toString().replace("-", "");
        try {
            Path dir = Paths.get(uploadDir, TEMP_DIR).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(token + ".xls").normalize();
            Files.write(target, bytes);
            pendingFiles.put(token, new PendingFile(target, originalName, parsed.marker()));
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "抽样单临时保存失败");
        }

        List<String> columns = new ArrayList<>();
        for (String column : parsed.columnOrder()) {
            columns.add(COLUMN_LABELS.getOrDefault(column, column));
        }

        return new PreviewResult(
                token,
                parsed.marker(),
                originalName,
                columns,
                parsed.columnOrder(),
                parsed.rows(),
                parsed.rows().size(),
                duplicated
        );
    }

    @Transactional
    public ImportResult confirmImport(String token, String userId) {
        PendingFile pending = pendingFiles.remove(token);
        if (pending == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "预览已过期或不存在，请重新上传抽样单");
        }

        ParsedSheet parsed;
        try {
            parsed = parse(Files.newInputStream(pending.path()));
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "待确认抽样单读取失败，请重新上传");
        } finally {
            try {
                Files.deleteIfExists(pending.path());
            } catch (IOException ignored) {
                // 临时文件清理失败不影响导入结果
            }
        }

        if (markerExists(parsed.marker())) {
            throw new BusinessException(
                    ResultCode.BAD_REQUEST.getCode(),
                    "抽样单标识「" + parsed.marker() + "」已导入过，禁止重复导入"
            );
        }

        List<String> columns = parsed.columnOrder();
        String sql = buildInsertSql(columns);
        List<Object[]> batchArgs = new ArrayList<>();
        for (Map<String, String> row : parsed.rows()) {
            Object[] args = new Object[columns.size()];
            for (int i = 0; i < columns.size(); i++) {
                String value = row.get(columns.get(i));
                args[i] = (value == null || value.isBlank()) ? null : value;
            }
            batchArgs.add(args);
        }

        jdbcTemplate.batchUpdate(sql, batchArgs);
        jdbcTemplate.update(
                "INSERT INTO sjtask_import_log(marker, original_name, row_count, created_by, created_at) VALUES (?, ?, ?, ?, ?)",
                parsed.marker(),
                pending.originalName(),
                batchArgs.size(),
                userId,
                Timestamp.valueOf(LocalDateTime.now())
        );

        return new ImportResult(parsed.marker(), batchArgs.size());
    }

    public byte[] buildTemplate() {
        try (Workbook workbook = new HSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);

            Row markerRow = sheet.createRow(0);
            markerRow.createCell(0).setCellValue("CYD-2026-001（请替换为本抽样单的唯一标识）");

            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < TEMPLATE_HEADERS.size(); i++) {
                headerRow.createCell(i).setCellValue(TEMPLATE_HEADERS.get(i));
            }

            Row exampleRow = sheet.createRow(2);
            String[] example = {
                    "sa-2609-001", "徐汇宏", "南通市市场监督局", "市级",
                    "市场监督局监抽任务，按文件要求执行", "2026.9.12",
                    "水产养殖监督抽检", "监督抽检", "2026.9.1", "2026.9.30",
                    "生产", "监抽全项"
            };
            for (int i = 0; i < example.length; i++) {
                exampleRow.createCell(i).setCellValue(example[i]);
            }

            for (int i = 0; i < TEMPLATE_HEADERS.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "模板生成失败");
        }
    }

    private boolean markerExists(String marker) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sjtask_import_log WHERE marker = ?",
                Integer.class,
                marker
        );
        return count != null && count > 0;
    }

    private String buildInsertSql(List<String> columns) {
        StringBuilder sql = new StringBuilder("INSERT INTO sjtask (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("`").append(columns.get(i)).append("`");
        }
        sql.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }
        sql.append(")");
        return sql.toString();
    }

    private ParsedSheet parse(InputStream inputStream) {
        try (Workbook workbook = new HSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                throw new BusinessException(
                        ResultCode.BAD_REQUEST.getCode(),
                        "工作簿中必须存在名为 \"sheet1\" 的工作表"
                );
            }

            Row firstRow = sheet.getRow(0);
            String marker = cellText(firstRow, 0);
            if (marker.isBlank()) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "A1 单元格必须填写文件标识，用于防止重复导入");
            }

            Row headerRow = sheet.getRow(1);
            if (headerRow == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "第 2 行必须为中文列标题行");
            }

            // 列序号 → sjtask 列名（TreeMap 保证按表中列顺序）
            TreeMap<Integer, String> columnIndexMap = new TreeMap<>();
            short lastCell = headerRow.getLastCellNum();
            for (int columnIndex = 0; columnIndex < lastCell; columnIndex++) {
                String header = cellText(headerRow, columnIndex)
                        .replace(" ", "")
                        .replace("　", "");
                String column = HEADER_ALIASES.get(header);
                if (column != null && !columnIndexMap.containsValue(column)) {
                    columnIndexMap.put(columnIndex, column);
                }
            }
            if (columnIndexMap.isEmpty()) {
                throw new BusinessException(
                        ResultCode.BAD_REQUEST.getCode(),
                        "第 2 行未识别到有效列标题，请参照模板填写（任务编号、责任人、任务来源等）"
                );
            }

            List<String> columnOrder = new ArrayList<>(columnIndexMap.values());
            List<Map<String, String>> rows = new ArrayList<>();
            int lastRowNum = sheet.getLastRowNum();
            for (int rowIndex = 2; rowIndex <= lastRowNum; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                Map<String, String> values = new LinkedHashMap<>();
                boolean hasValue = false;
                for (Map.Entry<Integer, String> entry : columnIndexMap.entrySet()) {
                    String value = cellText(row, entry.getKey());
                    if (!value.isBlank()) {
                        hasValue = true;
                    }
                    values.put(entry.getValue(), value);
                }
                if (hasValue) {
                    rows.add(values);
                }
            }

            if (rows.isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "第 3 行起未发现有效数据");
            }

            return new ParsedSheet(marker, columnOrder, rows);
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "无法解析该 .xls 文件，请确认文件未损坏且为 Excel 97-2003 格式");
        }
    }

    private String cellText(Row row, int columnIndex) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                && DateUtil.isCellDateFormatted(cell)) {
            return new java.text.SimpleDateFormat("yyyy-M-d").format(cell.getDateCellValue());
        }
        return dataFormatter.formatCellValue(cell).trim();
    }

    private record PendingFile(Path path, String originalName, String marker) {
    }

    /**
     * 解析后的抽样单
     */
    private record ParsedSheet(
            String marker,
            List<String> columnOrder,
            List<Map<String, String>> rows
    ) {
    }

    /**
     * 上传预览结果
     */
    public record PreviewResult(
            String token,
            String marker,
            String originalName,
            List<String> columns,
            List<String> columnKeys,
            List<Map<String, String>> rows,
            int total,
            boolean duplicated
    ) {
    }

    /**
     * 确认导入结果
     */
    public record ImportResult(String marker, int imported) {
    }
}
