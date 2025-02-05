package com.hanhome.youtube_comments.member.object;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class YoutubeAccountDetail {
    private String channelId;
    private String playlistId;
}
