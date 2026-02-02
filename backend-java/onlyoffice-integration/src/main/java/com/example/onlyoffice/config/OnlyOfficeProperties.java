package com.example.onlyoffice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

/**
 * ONLYOFFICE 配置属性类
 * 
 * 从 application.yml 中读取 onlyoffice.* 配置项
 * 所有配置都可通过环境变量覆盖
 * 
 * @author Your Name
 * @version 1.0.0
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "onlyoffice")
public class OnlyOfficeProperties {

    /**
     * 文档服务器配置
     */
    private DocumentServer documentServer = new DocumentServer();

    /**
     * 后端服务配置
     */
    private Backend backend = new Backend();

    /**
     * JWT 配置
     */
    private Jwt jwt = new Jwt();

    /**
     * 文件存储配置
     */
    private Storage storage = new Storage();

    /**
     * 文档服务器配置
     */
    @Data
    public static class DocumentServer {
        /**
         * ONLYOFFICE 文档服务器地址（前端访问）
         * 示例: http://localhost:8080 或 https://docs.example.com
         */
        @NotBlank(message = "文档服务器地址不能为空")
        private String url = "http://localhost:8080";

        /**
         * ONLYOFFICE 内部访问地址（可选，用于后端直接访问）
         */
        private String internalUrl;
    }

    /**
     * 后端服务配置
     */
    @Data
    public static class Backend {
        /**
         * 后端服务地址（对外访问）
         */
        @NotBlank(message = "后端服务地址不能为空")
        private String url = "http://localhost:3000";

        /**
         * 回调地址（ONLYOFFICE 容器访问后端）
         * Windows Docker: http://host.docker.internal:3000
         * Linux Docker: http://宿主机IP:3000
         */
        @NotBlank(message = "回调地址不能为空")
        private String callbackUrl = "http://host.docker.internal:3000";
    }

    /**
     * JWT 配置
     */
    @Data
    public static class Jwt {
        /**
         * JWT 密钥（必须与 ONLYOFFICE 配置一致）
         */
        @NotBlank(message = "JWT 密钥不能为空")
        private String secret = "your-super-secret-key-change-in-production";

        /**
         * Token 过期时间（秒）
         */
        private int expiresIn = 3600;

        /**
         * JWT Header 名称
         */
        private String header = "Authorization";
    }

    /**
     * 文件存储配置
     */
    @Data
    public static class Storage {
        /**
         * 文档存储目录
         */
        private String uploadDir = "./uploads";

        /**
         * 允许的文件类型（逗号分隔）
         */
        private String allowedTypes = "docx,xlsx,pptx,doc,xls,ppt,odt,ods,odp,pdf";

        /**
         * 最大文件大小（字节）
         */
        private long maxSize = 104857600L; // 100MB
    }
}
