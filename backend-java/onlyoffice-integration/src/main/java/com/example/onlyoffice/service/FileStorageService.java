package com.example.onlyoffice.service;

import com.example.onlyoffice.config.OnlyOfficeProperties;
import com.example.onlyoffice.exception.FileNotFoundException;
import com.example.onlyoffice.exception.FileStorageException;
import com.example.onlyoffice.exception.InvalidFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件存储服务
 * 
 * 负责文档的存储、读取、删除等操作
 * 包含安全校验，防止路径遍历攻击
 * 
 * @author Your Name
 * @version 1.0.0
 */
@Slf4j
@Service
public class FileStorageService {

    private final OnlyOfficeProperties properties;
    private Path uploadPath;
    private Set<String> allowedTypes;

    public FileStorageService(OnlyOfficeProperties properties) {
        this.properties = properties;
    }

    /**
     * 初始化存储目录
     */
    @PostConstruct
    public void init() {
        // 解析上传目录路径
        this.uploadPath = Paths.get(properties.getStorage().getUploadDir())
                .toAbsolutePath()
                .normalize();

        // 创建目录（如果不存在）
        try {
            Files.createDirectories(uploadPath);
            log.info("文件存储目录初始化完成: {}", uploadPath);
        } catch (IOException e) {
            throw new FileStorageException("无法创建上传目录: " + uploadPath, e);
        }

        // 解析允许的文件类型
        this.allowedTypes = Arrays.stream(properties.getStorage().getAllowedTypes().split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        
        log.info("允许的文件类型: {}", allowedTypes);
    }

    /**
     * 保存上传的文件
     * 
     * @param file 上传的文件
     * @return 保存后的文件名（不含扩展名）
     */
    public String saveFile(MultipartFile file) {
        // 获取原始文件名
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        
        // 验证文件
        validateFile(originalFilename, file.getSize());

        try {
            // 生成安全的文件名
            String safeFilename = generateSafeFilename(originalFilename);
            Path targetPath = uploadPath.resolve(safeFilename);

            // 保存文件
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("文件保存成功: {}", safeFilename);
            
            // 返回不含扩展名的文件名
            return removeExtension(safeFilename);
        } catch (IOException e) {
            throw new FileStorageException("保存文件失败: " + originalFilename, e);
        }
    }

    /**
     * 从 URL 下载并保存文件
     * 
     * @param inputStream 输入流
     * @param documentId 文档 ID
     * @param fileType 文件类型
     */
    public void saveFromStream(InputStream inputStream, String documentId, String fileType) {
        // 验证文档 ID
        String safeId = sanitizeDocumentId(documentId);
        if (safeId == null) {
            throw new InvalidFileException("无效的文档 ID: " + documentId);
        }

        String filename = safeId + "." + fileType;
        Path targetPath = uploadPath.resolve(filename);
        Path tempPath = uploadPath.resolve(safeId + "_temp_" + System.currentTimeMillis() + "." + fileType);

        try {
            // 先写入临时文件
            Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
            
            // 原子操作：移动临时文件覆盖原文件
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            
            log.info("文件保存成功: {}", filename);
        } catch (IOException e) {
            // 清理临时文件
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {}
            
            throw new FileStorageException("保存文件失败: " + filename, e);
        }
    }

    /**
     * 获取文件路径
     * 
     * @param documentId 文档 ID（不含扩展名）
     * @param fileType 文件类型
     * @return 文件路径
     */
    public Path getFilePath(String documentId, String fileType) {
        // 验证文档 ID
        String safeId = sanitizeDocumentId(documentId);
        if (safeId == null) {
            throw new InvalidFileException("无效的文档 ID: " + documentId);
        }

        String filename = safeId + "." + fileType;
        Path filePath = uploadPath.resolve(filename).normalize();

        // 安全检查：确保文件在上传目录内
        if (!filePath.startsWith(uploadPath)) {
            throw new InvalidFileException("非法的文件路径: " + documentId);
        }

        // 检查文件是否存在
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("文件不存在: " + filename);
        }

        return filePath;
    }

    /**
     * 获取文件最后修改时间
     * 
     * @param documentId 文档 ID
     * @param fileType 文件类型
     * @return 最后修改时间戳（毫秒）
     */
    public long getLastModifiedTime(String documentId, String fileType) {
        Path filePath = getFilePath(documentId, fileType);
        try {
            return Files.getLastModifiedTime(filePath).toMillis();
        } catch (IOException e) {
            throw new FileStorageException("获取文件修改时间失败", e);
        }
    }

    /**
     * 检查文件是否存在
     * 
     * @param documentId 文档 ID
     * @param fileType 文件类型
     * @return true 如果存在
     */
    public boolean fileExists(String documentId, String fileType) {
        String safeId = sanitizeDocumentId(documentId);
        if (safeId == null) {
            return false;
        }

        String filename = safeId + "." + fileType;
        Path filePath = uploadPath.resolve(filename).normalize();

        return filePath.startsWith(uploadPath) && Files.exists(filePath);
    }

    /**
     * 获取所有文档列表
     * 
     * @return 文档 ID 列表
     */
    public List<String> listDocuments() {
        try (Stream<Path> paths = Files.list(uploadPath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> !name.contains("_temp_"))
                    .filter(name -> {
                        String ext = getExtension(name).toLowerCase();
                        return allowedTypes.contains(ext);
                    })
                    .map(this::removeExtension)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new FileStorageException("获取文档列表失败", e);
        }
    }

    /**
     * 删除文件
     * 
     * @param documentId 文档 ID
     * @param fileType 文件类型
     */
    public void deleteFile(String documentId, String fileType) {
        Path filePath = getFilePath(documentId, fileType);
        try {
            Files.deleteIfExists(filePath);
            log.info("文件删除成功: {}.{}", documentId, fileType);
        } catch (IOException e) {
            throw new FileStorageException("删除文件失败", e);
        }
    }

    /**
     * 生成文档下载 URL
     * 
     * @param documentId 文档 ID
     * @param fileType 文件类型
     * @return 下载 URL
     */
    public String generateDownloadUrl(String documentId, String fileType) {
        String encodedFilename = URLEncoder.encode(documentId + "." + fileType, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return properties.getBackend().getCallbackUrl() + "/uploads/" + encodedFilename;
    }

    /**
     * 验证文件
     */
    private void validateFile(String filename, long size) {
        // 检查文件名
        if (filename == null || filename.isEmpty()) {
            throw new InvalidFileException("文件名不能为空");
        }

        // 检查文件类型
        String extension = getExtension(filename).toLowerCase();
        if (!allowedTypes.contains(extension)) {
            throw new InvalidFileException("不支持的文件类型: " + extension);
        }

        // 检查文件大小
        if (size > properties.getStorage().getMaxSize()) {
            throw new InvalidFileException("文件大小超过限制: " + size + " > " + properties.getStorage().getMaxSize());
        }
    }

    /**
     * 清理文档 ID，防止路径遍历攻击
     * 
     * @param documentId 原始文档 ID
     * @return 安全的文档 ID，如果无效则返回 null
     */
    public String sanitizeDocumentId(String documentId) {
        if (documentId == null || documentId.isEmpty()) {
            return null;
        }

        // 检查危险字符
        if (documentId.contains("..") || 
            documentId.contains("/") || 
            documentId.contains("\\") ||
            documentId.contains("\0")) {
            return null;
        }

        // URL 解码
        try {
            String decoded = java.net.URLDecoder.decode(documentId, StandardCharsets.UTF_8);
            // 再次检查解码后的内容
            if (decoded.contains("..") || decoded.contains("/") || decoded.contains("\\")) {
                return null;
            }
            return decoded;
        } catch (Exception e) {
            return documentId;
        }
    }

    /**
     * 生成安全的文件名
     */
    private String generateSafeFilename(String originalFilename) {
        // 移除路径分隔符
        String filename = originalFilename.replace("/", "").replace("\\", "");
        
        // 如果文件已存在，添加时间戳
        Path targetPath = uploadPath.resolve(filename);
        if (Files.exists(targetPath)) {
            String name = removeExtension(filename);
            String ext = getExtension(filename);
            filename = name + "_" + System.currentTimeMillis() + "." + ext;
        }
        
        return filename;
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex + 1) : "";
    }

    /**
     * 移除文件扩展名
     */
    private String removeExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }

    /**
     * 获取上传目录路径
     */
    public Path getUploadPath() {
        return uploadPath;
    }
}
