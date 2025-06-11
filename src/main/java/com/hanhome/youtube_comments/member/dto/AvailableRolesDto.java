package com.hanhome.youtube_comments.member.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

public class AvailableRolesDto {
    @Builder
    @Getter
    @Setter
    public static class Response implements Serializable {
        List<String> availableRoles;
    }
}
