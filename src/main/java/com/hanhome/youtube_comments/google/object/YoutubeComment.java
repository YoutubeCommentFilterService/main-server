package com.hanhome.youtube_comments.google.object;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class YoutubeComment {
    private String id;
    private String profileImage;
    private String nickname;
    private String comment;
    private String channelId;
}
