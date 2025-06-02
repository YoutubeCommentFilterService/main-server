package com.hanhome.youtube_comments.google.object.predict;

import com.hanhome.youtube_comments.google.object.YoutubeComment;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class PredictionCombinedResource {
    private String id;
    private String profileImage;
    private String nickname;
    private String comment;
    private String channelId;
    private Boolean isTopLevel;

    private String nicknamePredict;
    private List<Float> nicknameProb;
    private String commentPredict;
    private List<Float> commentProb;

    public PredictionCombinedResource(YoutubeComment originResource, PredictionResultResource predictResult) {
        this.id = originResource.getId();
        this.profileImage = originResource.getProfileImage();
        this.nickname = originResource.getNickname();
        this.comment = originResource.getComment();
        this.channelId = originResource.getChannelId();

        this.isTopLevel = this.id.length() <= 26;

        this.nicknamePredict = predictResult.getNicknamePredicted();
        this.nicknameProb = predictResult.getNicknamePredictProbs();
        this.commentPredict = predictResult.getCommentPredicted();
        this.commentProb = predictResult.getCommentPredictProbs();
    }
}
