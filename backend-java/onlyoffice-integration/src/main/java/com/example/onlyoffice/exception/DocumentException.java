package com.example.onlyoffice.exception;

/**
 * 文档处理异常
 * 
 * @author Your Name
 * @version 1.0.0
 */
public class DocumentException extends RuntimeException {

    public DocumentException(String message) {
        super(message);
    }

    public DocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
