package com.hanhome.youtube_comments.google.object;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PredictedCommentThread {
    private PredictResponseItem topLevelComment;
    private List<PredictResponseItem> replies;

    public PredictedCommentThread() {
        this.topLevelComment = null;
        replies = new ArrayList<>();
    }
}
