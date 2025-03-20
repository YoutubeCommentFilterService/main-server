package com.hanhome.youtube_comments.member.dto;

import lombok.Builder;
import lombok.Getter;

public class IsNewMemberDto {
    @Builder
    @Getter
    public static class Response {
        private Boolean isNewMember;
    }
}
