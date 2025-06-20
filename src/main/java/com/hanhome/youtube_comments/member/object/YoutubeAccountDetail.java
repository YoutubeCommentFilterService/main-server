package com.hanhome.youtube_comments.member.object;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class YoutubeAccountDetail {
    private String channelId;
    private String playlistId;
    private String channelName;
    private String channelHandler;
    private String thumbnailUrl;
    private Integer subscriberCount;

    @AllArgsConstructor
    @Getter
    public static class HandlerUrlMapper {
        private String thumbnailUrl;
        private String channelHandler;
        private Integer subscriberCount;
    }

    public HandlerUrlMapper mapToHandlerUrl() {
        return new HandlerUrlMapper(thumbnailUrl, channelHandler, subscriberCount);
    }
}
