package com.example.onlyoffice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * ONLYOFFICE 回调请求 DTO
 * 
 * 当文档状态变化时，ONLYOFFICE 会向 callbackUrl 发送此数据
 * 
 * @author Your Name
 * @version 1.0.0
 * @see <a href="https://api.onlyoffice.com/editors/callback">ONLYOFFICE Callback API</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CallbackDTO {

    /**
     * 文档状态码
     * 
     * 0 - 文档正在编辑中
     * 1 - 文档准备保存
     * 2 - 文档已保存，需要下载并保存到存储
     * 3 - 保存文档时发生错误
     * 4 - 文档关闭，无修改
     * 6 - 正在编辑，但当前状态已保存（强制保存）
     * 7 - 强制保存时发生错误
     */
    private Integer status;

    /**
     * 文档唯一标识（与请求时的 key 一致）
     */
    private String key;

    /**
     * 编辑后的文档下载地址
     * 仅当 status 为 2 或 6 时有效
     */
    private String url;

    /**
     * 变更历史下载地址
     */
    private String changesurl;

    /**
     * JWT Token（用于验证请求来源）
     */
    private String token;

    /**
     * 文档变更历史
     */
    private History history;

    /**
     * 当前编辑用户列表
     */
    private List<String> users;

    /**
     * 用户操作列表
     */
    private List<Action> actions;

    /**
     * 最后保存时间
     */
    private String lastsave;

    /**
     * 文档是否未修改
     */
    private Boolean notmodified;

    /**
     * 文件类型
     */
    private String filetype;

    /**
     * 强制保存类型
     * 0 - 服务器端强制保存
     * 1 - 客户端强制保存
     * 2 - 定时强制保存
     */
    private Integer forcesavetype;

    /**
     * 变更历史
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class History {
        private String serverVersion;
        private List<Change> changes;
    }

    /**
     * 单次变更记录
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Change {
        private String created;
        private User user;
    }

    /**
     * 用户信息
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        private String id;
        private String name;
    }

    /**
     * 用户操作
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Action {
        /**
         * 操作类型
         * 0 - 用户断开连接
         * 1 - 用户连接
         * 2 - 强制保存
         */
        private Integer type;
        private String userid;
    }

    /**
     * 判断是否需要保存文档
     * 
     * @return true 如果需要保存
     */
    public boolean needSave() {
        return status != null && (status == 2 || status == 6);
    }

    /**
     * 判断文档是否正在编辑
     * 
     * @return true 如果正在编辑
     */
    public boolean isEditing() {
        return status != null && status == 0;
    }

    /**
     * 判断是否发生错误
     * 
     * @return true 如果发生错误
     */
    public boolean hasError() {
        return status != null && (status == 3 || status == 7);
    }
}
