package com.example.onlyoffice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web 配置类
 * 
 * 配置跨域、静态资源等
 * 
 * @author Your Name
 * @version 1.0.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final OnlyOfficeProperties properties;

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    public WebConfig(OnlyOfficeProperties properties) {
        this.properties = properties;
    }

    /**
     * 配置跨域
     * 
     * 生产环境建议限制具体域名，而非使用 *
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins.split(","))
                .allowedMethods(allowedMethods.split(","))
                .allowedHeaders(allowedHeaders.split(","))
                .allowCredentials(allowCredentials)
                .maxAge(3600);
    }

    /**
     * 配置静态资源处理
     * 
     * 将 /uploads/** 映射到文件存储目录
     * ONLYOFFICE 通过此路径下载文档
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取上传目录的绝对路径
        String uploadPath = Paths.get(properties.getStorage().getUploadDir())
                .toAbsolutePath()
                .normalize()
                .toString();
        
        // 确保路径以 / 结尾
        if (!uploadPath.endsWith("/") && !uploadPath.endsWith("\\")) {
            uploadPath += "/";
        }

        // 映射 /uploads/** 到文件存储目录
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath)
                .setCachePeriod(0); // 禁用缓存，确保获取最新文件
    }
}
