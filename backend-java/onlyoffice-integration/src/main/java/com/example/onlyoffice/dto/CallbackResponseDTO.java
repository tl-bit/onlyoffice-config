package com.example.onlyoffice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ONLYOFFICE 回调响应 DTO
 * 
 * 回调处理完成后返回给 ONLYOFFICE 的响应
 * 
 * @author Your Name
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallbackResponseDTO {

    /**
     * 错误码
     * 0 - 成功
     * 非0 - 失败
     */
    private Integer error;

    /**
     * 错误消息（可选）
     */
    private String message;

    /**
     * 创建成功响应
     */
    public static CallbackResponseDTO success() {
        return new CallbackResponseDTO(0, null);
    }

    /**
     * 创建失败响应
     * 
     * @param message 错误消息
     */
    public static CallbackResponseDTO error(String message) {
        return new CallbackResponseDTO(1, message);
    }
}
