package com.example.onlyoffice.exception;

/**
 * 无效文件异常
 * 
 * @author Your Name
 * @version 1.0.0
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }

    public InvalidFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
