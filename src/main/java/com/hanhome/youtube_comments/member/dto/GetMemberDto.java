package com.hanhome.youtube_comments.member.dto;

import com.hanhome.youtube_comments.member.object.SimpleMemberInfo;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class GetMemberDto {
    @Builder
    @Getter
    @Setter
    public static class Request {
        @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
        private Integer page;
        @Min(value = 10, message = "한 페이지의 원소 개수는 10개 이상이어야 합니다.")
        private Integer take;

        private String channelName;
    }

    @Builder
    @Getter
    @Setter
    public static class Response {
        private Integer totalPages;
        private Long totalMembers;
        private List<SimpleMemberInfo> members;
    }
}
