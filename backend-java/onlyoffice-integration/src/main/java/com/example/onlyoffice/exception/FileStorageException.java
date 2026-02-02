package com.example.onlyoffice.exception;

/**
 * 文件存储异常
 * 
 * @author Your Name
 * @version 1.0.0
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
