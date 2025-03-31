package com.hanhome.youtube_comments.google.object;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class PredictionOutput {
    private String nicknamePredicted;
    private String commentPredicted;
    private List<Float> nicknameProb;
    private List<Float> commentProb;
}