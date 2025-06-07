package com.hanhome.youtube_comments.member.dto;

import lombok.Getter;
import lombok.Setter;

public class DeleteUserDto {
    @Getter
    @Setter
    public static class Request {
        private String channelId;
    }
}
