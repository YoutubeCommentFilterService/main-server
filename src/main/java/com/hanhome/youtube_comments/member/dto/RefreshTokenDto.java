package com.hanhome.youtube_comments.member.dto;

import com.hanhome.youtube_comments.member.object.MemberRole;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RefreshTokenDto {
    private String refreshToken;
    private String profileImage;
    private String nickname;
    private Boolean hasYoutubeAccess;
    private MemberRole role;
}
