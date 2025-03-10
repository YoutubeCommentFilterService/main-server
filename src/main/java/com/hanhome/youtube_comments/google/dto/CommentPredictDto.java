package com.hanhome.youtube_comments.google.dto;

import com.hanhome.youtube_comments.google.object.PredictTargetItem;
import lombok.*;

import java.util.List;

public class CommentPredictDto {
    @AllArgsConstructor
    @Getter
    public static class Request {
        private List<PredictTargetItem> items;
    }
}
