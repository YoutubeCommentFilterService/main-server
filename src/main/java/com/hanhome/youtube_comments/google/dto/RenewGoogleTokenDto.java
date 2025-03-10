package com.hanhome.youtube_comments.google.dto;

import lombok.*;

public class RenewGoogleTokenDto {
    @Builder
    @Getter
    public static class Request {
        private String clientId;
        private String clientSecret;
        private String grantType;
        private String refreshToken;
    }

    @Getter
    @Builder
    public static class Response {
        private String accessToken;
        private int expiresIn;
    }
}
