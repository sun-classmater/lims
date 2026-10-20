package com.lims.controller;

import com.lims.common.R;
import com.lims.service.SampleSheetImportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 样品管理 - 导入采样单（抽样单 .xls）
 * 步骤：上传抽样单到服务器 → 用户确认 → 确认无误导入数据库
 */
@RestController
@RequestMapping("/sample-sheet")
public class SampleSheetController {

    private final SampleSheetImportService sampleSheetImportService;

    public SampleSheetController(SampleSheetImportService sampleSheetImportService) {
        this.sampleSheetImportService = sampleSheetImportService;
    }

    /**
     * 步骤 1+2：上传抽样单并解析预览，返回待确认的数据行
     */
    @PostMapping("/preview")
    public R<SampleSheetImportService.PreviewResult> preview(
            @RequestParam("file") MultipartFile file
    ) {
        return R.ok(sampleSheetImportService.preview(file));
    }

    /**
     * 步骤 3：用户确认无误后正式导入 sjtask
     */
    @PostMapping("/import")
    public R<SampleSheetImportService.ImportResult> confirmImport(
            @RequestParam("token") String token,
            @RequestAttribute(value = "userId", required = false) String userId
    ) {
        return R.ok(sampleSheetImportService.confirmImport(token, userId));
    }

    /**
     * 下载抽样单 .xls 模板（A1 标识 + 第 2 行中文表头 + 第 3 行示例）
     */
    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        byte[] content = sampleSheetImportService.buildTemplate();
        String fileName = URLEncoder.encode("抽样单模板.xls", StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .contentLength(content.length)
                .body(content);
    }
}
