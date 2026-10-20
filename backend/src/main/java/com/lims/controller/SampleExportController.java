package com.lims.controller;

import com.lims.service.SampleExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 样品管理 - 导出数据。
 *
 * POST /api/sample-export/{type}  body: {"bhs": ["JK(2023)-SA-422", ...]}
 * type = water（水检上报） / agri（农检上报） / livestock（畜检上报） / detail（检验明细）
 */
@RestController
@RequestMapping("/sample-export")
public class SampleExportController {

    private final SampleExportService sampleExportService;

    public SampleExportController(SampleExportService sampleExportService) {
        this.sampleExportService = sampleExportService;
    }

    @PostMapping("/{type}")
    public ResponseEntity<byte[]> export(
            @PathVariable String type,
            @RequestBody(required = false) ExportRequest request
    ) {
        List<String> bhs = request == null ? List.of() : request.bhs();
        byte[] content = sampleExportService.buildExport(type, bhs);
        String fileName = sampleExportService.fileName(type);
        String asciiName = "sample-export-" + type + ".xls";
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.ms-excel"));
        headers.add(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + asciiName + "\"; filename*=UTF-8''" + encodedName
        );
        headers.setContentLength(content.length);
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    public record ExportRequest(List<String> bhs) {
    }
}
