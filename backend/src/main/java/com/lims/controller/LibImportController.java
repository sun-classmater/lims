package com.lims.controller;

import com.lims.common.R;
import com.lims.service.LibImportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 基础数据 - 项目库维护：导入检验项目库（lib.xls）。
 */
@RestController
@RequestMapping("/lib-import")
public class LibImportController {

    private final LibImportService libImportService;

    public LibImportController(LibImportService libImportService) {
        this.libImportService = libImportService;
    }

    /** 上传并解析，返回预览数据 */
    @PostMapping("/preview")
    public R<Map<String, Object>> preview(@RequestParam("file") MultipartFile file) {
        return R.ok(libImportService.preview(file));
    }

    /** 确认导入数据库 */
    @PostMapping("/confirm")
    public R<Map<String, Object>> confirm(@RequestBody(required = false) ConfirmRequest request) {
        return R.ok(libImportService.confirm(request == null ? null : request.token()));
    }

    /** 下载导入模板 */
    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        byte[] content = libImportService.buildTemplate();
        String fileName = URLEncoder.encode("项目库模板lib.xls", StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .contentLength(content.length)
                .body(content);
    }

    public record ConfirmRequest(String token) {
    }
}
