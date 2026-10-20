package com.lims.controller;

import com.lims.common.R;
import com.lims.service.FileLibraryService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 服务信息 - 中心文件资料（模板下载）。
 */
@RestController
@RequestMapping("/file-library")
public class FileLibraryController {

    private final FileLibraryService fileLibraryService;

    public FileLibraryController(FileLibraryService fileLibraryService) {
        this.fileLibraryService = fileLibraryService;
    }

    @GetMapping
    public R<List<Map<String, Object>>> list() {
        return R.ok(fileLibraryService.documents());
    }

    @GetMapping("/{type}")
    public ResponseEntity<byte[]> download(@PathVariable String type) {
        byte[] content = fileLibraryService.build(type);
        String fileName = fileLibraryService.fileName(type);
        String asciiName = "template-" + type + ".xls";
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.ms-excel"));
        headers.add(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + asciiName + "\"; filename*=UTF-8''" + encodedName
        );
        headers.setContentLength(content.length);
        return ResponseEntity.ok().headers(headers).body(content);
    }
}
