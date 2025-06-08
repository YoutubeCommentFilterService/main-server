package com.hanhome.youtube_comments.google.exception;

public class GoogleRefreshTokenNotFoundException extends RuntimeException {
    public GoogleRefreshTokenNotFoundException(String message) {
        super(message);
    }
}
