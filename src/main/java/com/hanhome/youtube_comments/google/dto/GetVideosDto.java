package com.hanhome.youtube_comments.google.dto;

import com.hanhome.youtube_comments.google.object.YoutubeVideo;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.List;

public class GetVideosDto {
    @AllArgsConstructor
    @Getter
    public static class Request {
        @Min(value = 1, message = "페이지는 1 이상입니다.")
        private Integer page;
    }

    @Setter
    @Getter
    @Builder
    public static class Response {
        private List<YoutubeVideo> items;
        private String isLast;
    }
}
