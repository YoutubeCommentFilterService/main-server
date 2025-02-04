package com.hanhome.youtube_comments.google.object;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class YoutubeComment {
    private String nextPageToken;
    private List<Comment> comments;

    @Getter
    @Builder
    public static class Comment {
        private String id;
        private String nickname;
        private String comment;
    }
}
