package com.hanhome.youtube_comments.google.dto;

import com.hanhome.youtube_comments.common.response.PredictCommonResponse;
import com.hanhome.youtube_comments.google.object.predict.PredictionInputResource;
import com.hanhome.youtube_comments.google.object.PredictionOutput;
import lombok.*;

import java.util.List;

public class CommentPredictDto {
    @Builder
    @Getter
    public static class Request {
        private List<PredictionInputResource> items;
    }

    @Builder
    @Getter
    public static class Response {
        private PredictCommonResponse predictCommonResponse;
        private List<PredictionOutput> results;
    }
}
