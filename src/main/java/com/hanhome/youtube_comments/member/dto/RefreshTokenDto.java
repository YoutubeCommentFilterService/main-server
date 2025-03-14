package com.hanhome.youtube_comments.member.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RefreshTokenDto {
    private String refreshToken;
    private String profileImage;
    private String nickname;
}
