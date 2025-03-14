package com.hanhome.youtube_comments.google.object;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CommentThreadPredictionResult {
    private PredictionResponse topLevelComment;
    private List<PredictionResponse> replies;

    public CommentThreadPredictionResult() {
        this.topLevelComment = null;
        replies = new ArrayList<>();
    }
}
