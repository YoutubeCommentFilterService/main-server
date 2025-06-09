package com.hanhome.youtube_comments.exception;

public class RenewAccessTokenFailedException extends RuntimeException{
    public RenewAccessTokenFailedException(String message) {
        super(message);
    }
}
