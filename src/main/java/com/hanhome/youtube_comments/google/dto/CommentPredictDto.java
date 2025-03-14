package com.hanhome.youtube_comments.google.dto;

import com.hanhome.youtube_comments.google.object.PredictionInput;
import com.hanhome.youtube_comments.google.object.PredictionOutput;
import lombok.*;

import java.util.List;

public class CommentPredictDto {
    @Builder
    @Getter
    public static class Request {
        private List<PredictionInput> items;
    }

    @Builder
    @Getter
    public static class Response {
        private List<PredictionOutput> results;
    }
}
