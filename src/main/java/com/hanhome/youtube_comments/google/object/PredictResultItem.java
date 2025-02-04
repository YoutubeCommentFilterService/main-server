package com.hanhome.youtube_comments.google.object;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PredictResultItem {
    private String id;
    private String nicknamePredicted;
    private String commentPredicted;
}