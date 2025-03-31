package com.hanhome.youtube_comments.google.object;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PredictionInput {
    private String nickname;
    private String comment;
}
