package com.example.onlyoffice.controller;

import com.example.onlyoffice.dto.CallbackDTO;
import com.example.onlyoffice.dto.CallbackResponseDTO;
import com.example.onlyoffice.dto.DocumentConfigDTO;
import com.example.onlyoffice.service.DocumentService;
import com.example.onlyoffice.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文档控制器
 * 
 * 提供文档管理和 ONLYOFFICE 集成的 REST API
 * 
 * @author Your Name
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class DocumentController {

    private final DocumentService documentService;
    private final FileStorageService fileStorageService;

    public DocumentController(DocumentService documentService, 
                             FileStorageService fileStorageService) {
        this.documentService = documentService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * 获取文档编辑器配置
     * 
     * 前端调用此接口获取 ONLYOFFICE 编辑器初始化配置
     * 
     * @param id 文档 ID（不含扩展名）
     * @param fileType 文件类型，默认 docx
     * @param userId 用户 ID（可选）
     * @param userName 用户名称（可选）
     * @param mode 编辑模式: edit（编辑）, view（只读），默认 edit
     * @return 编辑器配置
     * 
     * @apiNote 示例请求: GET /api/doc/test?fileType=docx&userId=user1&userName=张三&mode=edit
     */
    @GetMapping("/doc/{id}")
    public ResponseEntity<DocumentConfigDTO> getDocumentConfig(
            @PathVariable String id,
            @RequestParam(defaultValue = "docx") String fileType,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String userName,
            @RequestParam(defaultValue = "edit") String mode) {
        
        log.info("获取文档配置: id={}, fileType={}, userId={}, mode={}", id, fileType, userId, mode);
        
        DocumentConfigDTO config = documentService.getDocumentConfig(
                id, fileType, userId, userName, mode);
        
        return ResponseEntity.ok(config);
    }

    /**
     * ONLYOFFICE 回调接口
     * 
     * 当文档状态变化时（如保存、关闭），ONLYOFFICE 会调用此接口
     * 
     * @param callback 回调数据
     * @return 处理结果
     * 
     * @apiNote 此接口由 ONLYOFFICE 自动调用，无需手动调用
     */
    @PostMapping("/office/callback")
    public ResponseEntity<CallbackResponseDTO> handleCallback(@RequestBody CallbackDTO callback) {
        log.info("收到 ONLYOFFICE 回调: status={}, key={}", callback.getStatus(), callback.getKey());
        
        try {
            documentService.handleCallback(callback);
            return ResponseEntity.ok(CallbackResponseDTO.success());
        } catch (Exception e) {
            log.error("处理回调失败: {}", e.getMessage());
            return ResponseEntity.ok(CallbackResponseDTO.error(e.getMessage()));
        }
    }

    /**
     * 获取文档列表
     * 
     * @return 文档列表
     * 
     * @apiNote 示例响应:
     * [
     *   { "id": "test", "name": "test.docx" },
     *   { "id": "report", "name": "report.xlsx" }
     * ]
     */
    @GetMapping("/docs")
    public ResponseEntity<List<Map<String, String>>> listDocuments() {
        log.info("获取文档列表");
        
        List<String> documents = fileStorageService.listDocuments();
        
        List<Map<String, String>> result = documents.stream()
                .map(id -> {
                    Map<String, String> doc = new HashMap<>();
                    doc.put("id", id);
                    // 这里简化处理，实际可能需要查询文件类型
                    doc.put("name", id + ".docx");
                    return doc;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 上传文档
     * 
     * @param file 上传的文件
     * @return 上传结果
     * 
     * @apiNote 示例请求: POST /api/docs/upload
     *          Content-Type: multipart/form-data
     *          file: (binary)
     */
    @PostMapping("/docs/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(@RequestParam("file") MultipartFile file) {
        log.info("上传文档: {}", file.getOriginalFilename());
        
        String documentId = fileStorageService.saveFile(file);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("documentId", documentId);
        result.put("message", "上传成功");
        
        return ResponseEntity.ok(result);
    }

    /**
     * 删除文档
     * 
     * @param id 文档 ID
     * @param fileType 文件类型
     * @return 删除结果
     */
    @DeleteMapping("/docs/{id}")
    public ResponseEntity<Map<String, Object>> deleteDocument(
            @PathVariable String id,
            @RequestParam(defaultValue = "docx") String fileType) {
        
        log.info("删除文档: id={}, fileType={}", id, fileType);
        
        fileStorageService.deleteFile(id, fileType);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "删除成功");
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取 ONLYOFFICE 服务器信息
     * 
     * @return 服务器信息
     */
    @GetMapping("/office/info")
    public ResponseEntity<Map<String, String>> getOfficeInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("documentServerUrl", documentService.getDocumentServerUrl());
        info.put("apiUrl", documentService.getDocumentServerUrl() + "/web-apps/apps/api/documents/api.js");
        return ResponseEntity.ok(info);
    }

    /**
     * 健康检查接口
     * 
     * @return 服务状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "ok");
        health.put("timestamp", System.currentTimeMillis());
        health.put("service", "onlyoffice-integration");
        return ResponseEntity.ok(health);
    }
}
