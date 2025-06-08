package com.hanhome.youtube_comments.google.dto;

import com.hanhome.youtube_comments.common.response.PredictCommonResponse;
import com.hanhome.youtube_comments.google.object.predict.PredictionCombinedResource;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.List;

public class GetCommentsDto {
    @Getter
    @AllArgsConstructor
    public static class Request {
        @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
        private Integer page;
    }

    @Getter
    @Setter
    @Builder
    public static class Response {
        private PredictCommonResponse predictCommonResponse;
        private List<PredictionCombinedResource> items;
        private String isLast;
    }
}
