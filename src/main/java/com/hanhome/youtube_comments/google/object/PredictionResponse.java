package com.hanhome.youtube_comments.google.object;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Builder
public class PredictionResponse {
    private String id;
    private String profileImage;
    private String nickname;
    private String comment;
    private String nicknamePredict;
    private String commentPredict;
    private List<Float> nicknameProb;
    private List<Float> commentProb;
    private Boolean isTopLevel;

    public void setIsTopLevel(boolean isTopLevel) {
        this.isTopLevel = isTopLevel;
    }
}