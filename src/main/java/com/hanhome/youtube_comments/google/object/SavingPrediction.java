package com.hanhome.youtube_comments.google.object;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@SuperBuilder
public class SavingPrediction {
    private String id;
    private String profileImage;
    private String nickname;
    private String comment;
    private List<Float> nicknameProb;
    private List<Float> commentProb;
}
