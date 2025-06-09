package com.hanhome.youtube_comments.exception;

public class GoogleInvalidGrantException extends RuntimeException {
    public GoogleInvalidGrantException(String message) {
        super(message);
    }
}
