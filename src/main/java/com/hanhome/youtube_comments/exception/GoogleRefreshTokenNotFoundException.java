package com.hanhome.youtube_comments.exception;

public class GoogleRefreshTokenNotFoundException extends RuntimeException {
    public GoogleRefreshTokenNotFoundException(String message) {
        super(message);
    }
}
