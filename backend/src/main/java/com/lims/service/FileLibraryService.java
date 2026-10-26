package com.lims.service;

import com.lims.common.BusinessException;
import com.lims.common.ResultCode;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务信息 - 中心文件资料。
 *
 * 提供采样信息导入模板与项目库模板的下载：
 * - sample：复用抽样单导入的模板（sheet1 + 文件标识 + 中文表头 + 示例行）；
 * - lib：按 lib 表结构生成的检验项目库模板。
 */
@Service
public class FileLibraryService {

    private final SampleSheetImportService sampleSheetImportService;
    private final FileStorageService fileStorageService;
    private final LibImportService libImportService;

    public FileLibraryService(
            SampleSheetImportService sampleSheetImportService,
            FileStorageService fileStorageService,
            LibImportService libImportService
    ) {
        this.sampleSheetImportService = sampleSheetImportService;
        this.fileStorageService = fileStorageService;
        this.libImportService = libImportService;
    }

    /**
     * 启动时把两个内置模板落到上传目录，便于“中心资料上传管理”统一展示、
     * 下载与删除；已存在同名模板时跳过。
     */
    @PostConstruct
    public void seedTemplates() {
        try {
            seed("sample");
            seed("lib");
        } catch (Exception exception) {
            // 模板写入失败不阻塞启动，仍可通过原有的 /file-library 接口下载
        }
    }

    private void seed(String type) {
        String name = fileName(type);
        String existing = fileStorageService.storedNameOf(name);
        if (existing != null) {
            fileStorageService.delete(existing);
        }
        fileStorageService.storeBytes(name, build(type));
    }

    public List<Map<String, Object>> documents() {
        return List.of(
                document("sample", "【0】采样信息【导入模板】.xls"),
                document("lib", "项目库模板lib.xls")
        );
    }

    public byte[] build(String type) {
        if ("sample".equals(type)) {
            return sampleSheetImportService.buildTemplate();
        }
        if ("lib".equals(type)) {
            return libImportService.buildTemplate();
        }
        throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "未知的模板类型: " + type);
    }

    public String fileName(String type) {
        if ("sample".equals(type)) {
            return "【0】采样信息【导入模板】.xls";
        }
        if ("lib".equals(type)) {
            return "项目库模板lib.xls";
        }
        throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "未知的模板类型: " + type);
    }

    private Map<String, Object> document(String type, String name) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", type);
        item.put("name", name);
        return item;
    }
}
