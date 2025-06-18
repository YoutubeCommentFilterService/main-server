package com.hanhome.youtube_comments.member.dto;

import com.hanhome.youtube_comments.member.object.MemberRole;
import com.hanhome.youtube_comments.oauth.dto.CustomTokenRecord;
import lombok.Builder;
import lombok.Getter;

public class AccessTokenDto {
    @Builder
    @Getter
    public static class Renew {
        private CustomTokenRecord customTokenRecord;
        private String profileImage;
        private String nickname;
        private Boolean hasYoutubeAccess;
        private MemberRole role;
    }
}
