package com.hanhome.youtube_comments.oauth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RenewAccessTokenDto {
    private String refreshToken;
}
