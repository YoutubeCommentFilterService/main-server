package com.hanhome.youtube_comments.google.object.predict;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PredictionInputResource {
    private String nickname;
    private String comment;
}
