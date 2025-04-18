package com.hanhome.youtube_comments.google.dto;

import com.hanhome.youtube_comments.google.object.YoutubeVideo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.springframework.lang.Nullable;

import java.util.List;

public class GetVideosDto {
    @AllArgsConstructor
    @Getter
    public static class Request {
        @Min(value = 1, message = "페이지는 1 이상입니다.")
        private Integer page;

        @Nullable
        @Min(value = 1, message = "가져오는 개수는 1 이상입니다.")
        @Max(value = 50, message = "가져오는 개수는 50 이하입니다.")
        private Integer take;
    }

    @Setter
    @Getter
    @Builder
    public static class Response {
        private List<YoutubeVideo> items;
        private String isLast;
    }

    @Getter
    @Setter
    @Builder
    public static class FromGoogle {
        private Boolean isLast;
        private String nextPageToken;
        private List<YoutubeVideo> videoResources;
    }
}
