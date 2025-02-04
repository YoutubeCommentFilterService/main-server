package com.hanhome.youtube_comments.google.object;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PredictTargetItem {
    private String id;
    private String nickname;
    private String comment;
}
