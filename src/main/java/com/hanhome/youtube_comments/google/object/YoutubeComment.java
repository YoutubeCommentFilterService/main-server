package com.hanhome.youtube_comments.google.object;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class YoutubeComment {
    private String id;
    private String profileImage;
    private String nickname;
    private String comment;
}
