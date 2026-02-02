package com.example.onlyoffice.exception;

/**
 * 文件未找到异常
 * 
 * @author Your Name
 * @version 1.0.0
 */
public class FileNotFoundException extends RuntimeException {

    public FileNotFoundException(String message) {
        super(message);
    }

    public FileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
