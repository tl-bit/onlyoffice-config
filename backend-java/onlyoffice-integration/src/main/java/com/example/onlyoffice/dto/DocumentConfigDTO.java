package com.example.onlyoffice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ONLYOFFICE 文档配置 DTO
 * 
 * 用于返回给前端的编辑器初始化配置
 * 包含文档信息、编辑器配置、JWT Token 等
 * 
 * @author Your Name
 * @version 1.0.0
 * @see <a href="https://api.onlyoffice.com/editors/config/">ONLYOFFICE Config API</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentConfigDTO {

    /**
     * 文档信息
     */
    private Document document;

    /**
     * 编辑器配置
     */
    private EditorConfig editorConfig;

    /**
     * 文档类型: word, cell, slide
     */
    private String documentType;

    /**
     * JWT Token（用于安全验证）
     */
    private String token;

    /**
     * 编辑器宽度
     */
    private String width;

    /**
     * 编辑器高度
     */
    private String height;

    /**
     * 文档信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Document {
        /**
         * 文件类型: docx, xlsx, pptx 等
         */
        private String fileType;

        /**
         * 文档唯一标识（用于版本控制）
         * 当文档内容变化时，key 也应该变化
         */
        private String key;

        /**
         * 文档标题（显示在编辑器标题栏）
         */
        private String title;

        /**
         * 文档下载地址（ONLYOFFICE 从此地址获取文档）
         */
        private String url;

        /**
         * 文档权限配置
         */
        private Permissions permissions;
    }

    /**
     * 文档权限配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Permissions {
        /**
         * 是否允许下载
         */
        private Boolean download;

        /**
         * 是否允许编辑
         */
        private Boolean edit;

        /**
         * 是否允许打印
         */
        private Boolean print;

        /**
         * 是否允许审阅
         */
        private Boolean review;

        /**
         * 是否允许评论
         */
        private Boolean comment;

        /**
         * 是否允许填写表单
         */
        private Boolean fillForms;
    }

    /**
     * 编辑器配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EditorConfig {
        /**
         * 回调地址（文档保存时 ONLYOFFICE 调用）
         */
        private String callbackUrl;

        /**
         * 界面语言: zh-CN, en-US 等
         */
        private String lang;

        /**
         * 编辑模式: edit, view
         */
        private String mode;

        /**
         * 用户信息
         */
        private User user;

        /**
         * 自定义配置
         */
        private Customization customization;
    }

    /**
     * 用户信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User {
        /**
         * 用户 ID
         */
        private String id;

        /**
         * 用户名称（显示在编辑器中）
         */
        private String name;
    }

    /**
     * 自定义配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Customization {
        /**
         * 是否启用自动保存
         */
        private Boolean autosave;

        /**
         * 是否启用强制保存
         */
        private Boolean forcesave;

        /**
         * 是否显示聊天
         */
        private Boolean chat;

        /**
         * 是否显示评论
         */
        private Boolean comments;

        /**
         * 是否显示帮助
         */
        private Boolean help;

        /**
         * 是否紧凑模式
         */
        private Boolean compactToolbar;
    }
}
