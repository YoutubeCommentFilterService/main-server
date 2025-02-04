package com.hanhome.youtube_comments.google.dto;

import com.hanhome.youtube_comments.google.object.PredictTargetItem;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

public class CommentPredictDto {
    @AllArgsConstructor
    @Getter
    public static class Request {
        private List<PredictTargetItem> items;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class Response {
        private List<PredictResult> items;

        @Builder
        @Getter
        public static class PredictResult {
            private String id;
            private String nicknamePredicted;
            private String commentPredicted;
        }
    }
}
