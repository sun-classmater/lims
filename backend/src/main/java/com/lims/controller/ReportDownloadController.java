package com.lims.controller;

import com.lims.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 样品管理 - 报告下载。
 *
 * GET /api/report-download/{type}?bh=样品编号
 * type = cma（CMA 报告） / catl（CMA_CATL 报告）
 */
@RestController
@RequestMapping("/report-download")
public class ReportDownloadController {

    private final ReportService reportService;

    public ReportDownloadController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/{type}")
    public ResponseEntity<byte[]> download(
            @PathVariable String type,
            @RequestParam("bh") String bh
    ) {
        byte[] content = reportService.buildReport(type, bh);
        String fileName = reportService.fileName(type, bh);
        String asciiName = "report-" + type + ".doc";
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/msword"));
        headers.add(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + asciiName + "\"; filename*=UTF-8''" + encodedName
        );
        headers.setContentLength(content.length);
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }
}
