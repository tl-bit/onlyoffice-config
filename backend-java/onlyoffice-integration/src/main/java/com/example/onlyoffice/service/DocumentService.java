package com.example.onlyoffice.service;

import com.example.onlyoffice.config.OnlyOfficeProperties;
import com.example.onlyoffice.dto.CallbackDTO;
import com.example.onlyoffice.dto.DocumentConfigDTO;
import com.example.onlyoffice.exception.DocumentException;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 文档服务类
 * 
 * 负责生成 ONLYOFFICE 编辑器配置和处理回调
 * 
 * @author Your Name
 * @version 1.0.0
 */
@Slf4j
@Service
public class DocumentService {

    private final OnlyOfficeProperties properties;
    private final JwtService jwtService;
    private final FileStorageService fileStorageService;

    public DocumentService(OnlyOfficeProperties properties, 
                          JwtService jwtService, 
                          FileStorageService fileStorageService) {
        this.properties = properties;
        this.jwtService = jwtService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * 获取文档编辑器配置
     * 
     * @param documentId 文档 ID（不含扩展名）
     * @param fileType 文件类型（如 docx）
     * @param userId 用户 ID
     * @param userName 用户名称
     * @param mode 编辑模式: edit, view
     * @return 编辑器配置
     */
    public DocumentConfigDTO getDocumentConfig(String documentId, 
                                                String fileType,
                                                String userId, 
                                                String userName,
                                                String mode) {
        // 验证文件是否存在
        if (!fileStorageService.fileExists(documentId, fileType)) {
            throw new DocumentException("文档不存在: " + documentId);
        }

        // 获取文件最后修改时间作为版本标识
        long lastModified = fileStorageService.getLastModifiedTime(documentId, fileType);

        // 生成文档唯一 key
        // ONLYOFFICE 的 key 只能包含 [0-9a-zA-Z.=_-]，最长 128 字符
        String documentKey = generateDocumentKey(documentId, lastModified);

        // 生成文档下载 URL
        String documentUrl = fileStorageService.generateDownloadUrl(documentId, fileType);

        // 生成回调 URL
        String callbackUrl = properties.getBackend().getCallbackUrl() + "/api/office/callback";

        // 构建配置对象
        DocumentConfigDTO config = DocumentConfigDTO.builder()
                .document(DocumentConfigDTO.Document.builder()
                        .fileType(fileType)
                        .key(documentKey)
                        .title(documentId + "." + fileType)
                        .url(documentUrl)
                        .permissions(DocumentConfigDTO.Permissions.builder()
                                .download(true)
                                .edit("edit".equals(mode))
                                .print(true)
                                .review(true)
                                .comment(true)
                                .fillForms(true)
                                .build())
                        .build())
                .editorConfig(DocumentConfigDTO.EditorConfig.builder()
                        .callbackUrl(callbackUrl)
                        .lang("zh-CN")
                        .mode(mode != null ? mode : "edit")
                        .user(DocumentConfigDTO.User.builder()
                                .id(userId != null ? userId : "anonymous")
                                .name(userName != null ? userName : "匿名用户")
                                .build())
                        .customization(DocumentConfigDTO.Customization.builder()
                                .autosave(true)
                                .forcesave(true)
                                .chat(false)
                                .comments(true)
                                .help(true)
                                .compactToolbar(false)
                                .build())
                        .build())
                .documentType(getDocumentType(fileType))
                .width("100%")
                .height("100%")
                .build();

        // 生成 JWT Token（只对 document、editorConfig、documentType 签名）
        Map<String, Object> tokenPayload = new HashMap<>();
        tokenPayload.put("document", config.getDocument());
        tokenPayload.put("editorConfig", config.getEditorConfig());
        tokenPayload.put("documentType", config.getDocumentType());
        
        String token = jwtService.createToken(tokenPayload);
        config.setToken(token);

        log.info("生成文档配置: documentId={}, key={}", documentId, documentKey);

        return config;
    }

    /**
     * 处理 ONLYOFFICE 回调
     * 
     * @param callback 回调数据
     */
    public void handleCallback(CallbackDTO callback) {
        log.info("收到回调: status={}, key={}", callback.getStatus(), callback.getKey());

        // 验证 JWT Token（如果存在）
        if (callback.getToken() != null && !callback.getToken().isEmpty()) {
            if (!jwtService.isTokenValid(callback.getToken())) {
                throw new DocumentException("JWT Token 验证失败");
            }
            log.debug("JWT Token 验证成功");
        }

        // 根据状态处理
        if (callback.needSave()) {
            // 需要保存文档
            saveDocument(callback);
        } else if (callback.isEditing()) {
            // 文档正在编辑中
            log.debug("文档正在编辑中: key={}", callback.getKey());
        } else if (callback.hasError()) {
            // 发生错误
            log.error("文档保存错误: key={}, status={}", callback.getKey(), callback.getStatus());
        } else {
            // 其他状态（如文档关闭无修改）
            log.debug("文档状态变更: key={}, status={}", callback.getKey(), callback.getStatus());
        }
    }

    /**
     * 保存文档
     * 
     * @param callback 回调数据
     */
    private void saveDocument(CallbackDTO callback) {
        String url = callback.getUrl();
        String key = callback.getKey();
        String fileType = callback.getFiletype();

        if (url == null || url.isEmpty()) {
            throw new DocumentException("回调中缺少文档 URL");
        }

        // 从 key 中提取文档 ID
        String documentId = extractDocumentIdFromKey(key);
        if (documentId == null) {
            throw new DocumentException("无法从 key 中提取文档 ID: " + key);
        }

        // 如果没有指定文件类型，默认使用 docx
        if (fileType == null || fileType.isEmpty()) {
            fileType = "docx";
        }

        log.info("开始保存文档: documentId={}, url={}", documentId, url);

        // 下载文档
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            
            byte[] content = httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                if (statusCode != 200) {
                    throw new DocumentException("下载文档失败，状态码: " + statusCode);
                }
                return EntityUtils.toByteArray(response.getEntity());
            });

            // 保存文档
            fileStorageService.saveFromStream(
                    new ByteArrayInputStream(content), 
                    documentId, 
                    fileType
            );

            log.info("文档保存成功: documentId={}", documentId);
        } catch (IOException e) {
            log.error("保存文档失败: {}", e.getMessage());
            throw new DocumentException("保存文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成文档唯一 key
     * 
     * ONLYOFFICE 使用 key 来识别文档版本
     * key 变化时会重新加载文档
     * 
     * @param documentId 文档 ID
     * @param lastModified 最后修改时间
     * @return 文档 key
     */
    private String generateDocumentKey(String documentId, long lastModified) {
        // 对中文文件名进行 Base64 编码
        String encodedId = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(documentId.getBytes(StandardCharsets.UTF_8));
        
        // 组合 key: base64(documentId)_timestamp
        return encodedId + "_" + lastModified;
    }

    /**
     * 从 key 中提取文档 ID
     * 
     * @param key 文档 key
     * @return 文档 ID
     */
    private String extractDocumentIdFromKey(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        // key 格式: base64(documentId)_timestamp
        int lastUnderscore = key.lastIndexOf('_');
        if (lastUnderscore <= 0) {
            return null;
        }

        String encodedId = key.substring(0, lastUnderscore);
        
        try {
            // Base64 解码
            byte[] decoded = Base64.getUrlDecoder().decode(encodedId);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("解码文档 ID 失败: {}", encodedId);
            return null;
        }
    }

    /**
     * 根据文件类型获取文档类型
     * 
     * @param fileType 文件类型
     * @return 文档类型: word, cell, slide
     */
    private String getDocumentType(String fileType) {
        if (fileType == null) {
            return "word";
        }

        switch (fileType.toLowerCase()) {
            case "xlsx":
            case "xls":
            case "ods":
            case "csv":
                return "cell";
            case "pptx":
            case "ppt":
            case "odp":
                return "slide";
            default:
                return "word";
        }
    }

    /**
     * 获取 ONLYOFFICE 文档服务器地址
     * 
     * @return 文档服务器 URL
     */
    public String getDocumentServerUrl() {
        return properties.getDocumentServer().getUrl();
    }
}
