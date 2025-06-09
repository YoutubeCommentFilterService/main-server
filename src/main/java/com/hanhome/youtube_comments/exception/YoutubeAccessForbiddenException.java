package com.hanhome.youtube_comments.exception;

public class YoutubeAccessForbiddenException extends RuntimeException {
    public YoutubeAccessForbiddenException(String message) {
        super(message);
    }

    public YoutubeAccessForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
