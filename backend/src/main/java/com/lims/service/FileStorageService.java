package com.lims.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lims.common.BusinessException;
import com.lims.common.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 中心资料文件存储。
 *
 * 文件以 UUID 命名保存在 uploads 目录，原始文件名记录在 .index.json 中，
 * 这样列表可以展示原始名称，删除时也能安全定位到物理文件。
 */
@Service
public class FileStorageService {

    private static final String INDEX_FILE = ".index.json";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${lims.upload-dir:./uploads}")
    private String uploadDir;

    private final ObjectMapper objectMapper;

    public FileStorageService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized Map<String, Object> store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件不能为空");
        }
        String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        try {
            return save(originalName, file.getBytes());
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "文件上传失败");
        }
    }

    /** 写入文件内容（用于生成内置模板），返回存储信息 */
    public synchronized Map<String, Object> storeBytes(String originalName, byte[] content) {
        try {
            return save(originalName, content);
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "文件写入失败");
        }
    }

    public List<Map<String, Object>> listFiles() {
        Path root = root();
        if (!Files.exists(root)) {
            return new ArrayList<>();
        }
        Map<String, Map<String, Object>> index = readIndex();
        List<Map<String, Object>> files = new ArrayList<>();
        try {
            Files.list(root)
                    .filter(Files::isRegularFile)
                    .filter(path -> !INDEX_FILE.equals(path.getFileName().toString()))
                    .sorted(Comparator.comparing(path -> originalName(index, path.getFileName().toString())))
                    .forEach(path -> {
                        String name = path.getFileName().toString();
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("name", name);
                        item.put("originalName", originalName(index, name));
                        item.put("size", sizeOf(path));
                        item.put("uploadedAt", text(index.get(name), "uploadedAt"));
                        item.put("url", "/api/files/" + name);
                        files.add(item);
                    });
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "文件列表读取失败");
        }
        return files;
    }

    public boolean existsByOriginalName(String originalName) {
        for (Map<String, Object> item : listFiles()) {
            if (originalName.equals(item.get("originalName"))) {
                return true;
            }
        }
        return false;
    }

    /** 按原始文件名查找实际存储名，找不到返回 null */
    public String storedNameOf(String originalName) {
        for (Map<String, Object> item : listFiles()) {
            if (originalName.equals(item.get("originalName"))) {
                return String.valueOf(item.get("name"));
            }
        }
        return null;
    }

    public String originalNameOf(String storedName) {
        return originalName(readIndex(), storedName);
    }

    public synchronized int delete(String storedName) {
        String name = storedName == null ? "" : storedName.trim();
        if (name.isEmpty() || name.contains("/") || name.contains("\\") || INDEX_FILE.equals(name)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "非法的文件名");
        }
        Path target = root().resolve(name).normalize();
        if (!target.startsWith(root())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "非法的文件名");
        }
        try {
            boolean deleted = Files.deleteIfExists(target);
            Map<String, Map<String, Object>> index = readIndex();
            if (index.remove(name) != null) {
                writeIndex(index);
            }
            return deleted ? 1 : 0;
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "文件删除失败");
        }
    }

    private Map<String, Object> save(String originalName, byte[] content) throws IOException {
        Path root = root();
        Files.createDirectories(root);
        String suffix = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.'))
                : "";
        String storedName = UUID.randomUUID() + suffix;
        Files.write(root.resolve(storedName).normalize(), content);

        Map<String, Map<String, Object>> index = readIndex();
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("originalName", originalName);
        meta.put("size", content.length);
        meta.put("uploadedAt", LocalDateTime.now().format(TIME_FORMAT));
        index.put(storedName, meta);
        writeIndex(index);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", storedName);
        result.put("storedName", storedName);
        result.put("originalName", originalName);
        result.put("size", content.length);
        result.put("url", "/api/files/" + storedName);
        return result;
    }

    private Map<String, Map<String, Object>> readIndex() {
        Path indexFile = root().resolve(INDEX_FILE);
        if (!Files.exists(indexFile)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(
                    indexFile.toFile(),
                    new TypeReference<LinkedHashMap<String, Map<String, Object>>>() { }
            );
        } catch (IOException exception) {
            return new LinkedHashMap<>();
        }
    }

    private void writeIndex(Map<String, Map<String, Object>> index) throws IOException {
        Path root = root();
        Files.createDirectories(root);
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(root.resolve(INDEX_FILE).toFile(), index);
    }

    private Path root() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private static String originalName(Map<String, Map<String, Object>> index, String storedName) {
        String name = text(index.get(storedName), "originalName");
        return name.isEmpty() ? storedName : name;
    }

    private static String text(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static long sizeOf(Path path) {
        try {
            return Files.size(path);
        } catch (IOException exception) {
            return 0;
        }
    }
}
