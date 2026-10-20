package com.lims.service;

import com.lims.common.BusinessException;
import com.lims.common.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${lims.upload-dir:./uploads}")
    private String uploadDir;

    public Map<String, Object> store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件不能为空");
        }

        try {
            Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(root);
            String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String suffix = originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf('.'))
                    : "";
            String storedName = UUID.randomUUID() + suffix;
            Path target = root.resolve(storedName).normalize();
            file.transferTo(target);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("originalName", originalName);
            result.put("storedName", storedName);
            result.put("size", file.getSize());
            result.put("url", "/api/files/" + storedName);
            return result;
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "文件上传失败");
        }
    }

    public List<Map<String, Object>> listFiles() {
        try {
            Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(root)) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> files = new ArrayList<>();
            Files.list(root)
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("name", path.getFileName().toString());
                        item.put("url", "/api/files/" + path.getFileName().toString());
                        files.add(item);
                    });
            return files;
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "文件列表读取失败");
        }
    }
}
