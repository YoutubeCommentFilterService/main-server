package com.hanhome.youtube_comments.google.dto;

import com.hanhome.youtube_comments.google.object.PredictResponseItem;
import com.hanhome.youtube_comments.google.object.YoutubeComment;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.springframework.lang.Nullable;

import java.util.List;

public class GetCommentsDto {
    @Getter
    @AllArgsConstructor
    public static class Request {
        @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
        private Integer page;

        @Nullable
        @Min(value = 1, message = "가져오는 개수는 1 이상입니다.")
        @Max(value = 100, message = "가져오는 개수는 100 이하입니다.")
        private Integer take;
    }

    @Getter
    @Setter
    @Builder
    public static class Response {
        private List<PredictResponseItem> items;
        private String isLast;
    }

    @Getter
    @Builder
    public static class FromGoogle {
        private String nextPageToken;
        private List<YoutubeComment> comments;
    }
}
