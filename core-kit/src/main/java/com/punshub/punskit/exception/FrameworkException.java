package com.punshub.punskit.exception;

/**
 * Exception gốc của framework — tất cả exception khác kế thừa từ đây.
 * Là RuntimeException nên không cần khai báo throws.
 */
public class FrameworkException extends RuntimeException {

    public FrameworkException(String message) {
        super(message);
    }

    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
