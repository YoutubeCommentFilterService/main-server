package com.hanhome.youtube_comments.google.object;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@SuperBuilder
public class PredictionResponse extends SavingPrediction {
    private String channelId;
    private String nicknamePredict;
    private String commentPredict;
    private Boolean isTopLevel;

    public void setIsTopLevel(boolean isTopLevel) {
        this.isTopLevel = isTopLevel;
    }
}