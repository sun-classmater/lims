package com.lims.controller;

import com.lims.common.R;
import com.lims.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileStorageService fileStorageService;

    @Value("${lims.upload-dir:./uploads}")
    private String uploadDir;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        return R.ok(fileStorageService.store(file));
    }

    @GetMapping
    public R<List<Map<String, Object>>> list() {
        return R.ok(fileStorageService.listFiles());
    }

    @GetMapping("/{name}")
    public ResponseEntity<Resource> download(@PathVariable String name) {
        try {
            Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path file = root.resolve(name).normalize();
            if (!file.startsWith(root) || !Files.exists(file)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(file.toUri());
            String originalName = fileStorageService.originalNameOf(name);
            String encodedName = URLEncoder.encode(originalName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            String asciiName = "file" + (name.contains(".")
                    ? name.substring(name.lastIndexOf('.'))
                    : "");
            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + asciiName + "\"; filename*=UTF-8''" + encodedName
                    )
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{name}")
    public R<Integer> delete(@PathVariable String name) {
        return R.ok(fileStorageService.delete(name));
    }
}
