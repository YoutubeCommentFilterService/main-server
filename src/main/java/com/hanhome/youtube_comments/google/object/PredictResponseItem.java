package com.hanhome.youtube_comments.google.object;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@AllArgsConstructor
@Getter
public class PredictResponseItem {
    private String id;
    private String nickname;
    private String comment;
    private String nicknamePredict;
    private String commentPredict;
    private List<Float> nicknameProb;
    private List<Float> commentProb;
}