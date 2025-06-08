package com.hanhome.youtube_comments.member.object;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class YoutubeAccountDetail {
    private String channelId;
    private String playlistId;
//    private String channelName;
    private String channelHandler;
//    private String thumbnailUrl;
}
