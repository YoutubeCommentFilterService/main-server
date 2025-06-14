package com.hanhome.youtube_comments.member.dto;

import com.hanhome.youtube_comments.common.enum_validate.ValidEnum;
import com.hanhome.youtube_comments.member.object.MemberRole;
import lombok.Getter;
import lombok.Setter;

public class UpdateUserRoleDto {
    @Getter
    @Setter
    public static class Request {
        private String userId;
        @ValidEnum(enumClass = MemberRole.class)
        private String role;
    }
}
