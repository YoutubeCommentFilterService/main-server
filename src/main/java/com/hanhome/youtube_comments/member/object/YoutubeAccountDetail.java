package com.hanhome.youtube_comments.member.object;

import com.hanhome.youtube_comments.google.object.youtube_data_api.channel.ChannelContentDetailsResource;
import com.hanhome.youtube_comments.google.object.youtube_data_api.channel.ChannelResource;
import com.hanhome.youtube_comments.google.object.youtube_data_api.channel.ChannelSnippetResource;
import com.hanhome.youtube_comments.google.object.youtube_data_api.channel.ChannelStatisticsResource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
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

    public YoutubeAccountDetail(ChannelResource channelResource) {
        ChannelSnippetResource channelSnippetResource = channelResource.getSnippet();
        ChannelContentDetailsResource channelContentDetailsResource = channelResource.getContentDetails();

        channelId = channelResource.getId();
        playlistId = channelContentDetailsResource.getRelatedPlaylists().getUploads();
        channelHandler = channelSnippetResource.getCustomUrl();
        channelName = channelSnippetResource.getTitle();
        thumbnailUrl = channelSnippetResource.getThumbnail();

        ChannelStatisticsResource channelStatistics = channelResource.getStatistics();
        if (channelResource.getStatistics() != null) {
            subscriberCount = channelStatistics.getSubscriberCount();
        }
    }

    public HandlerUrlMapper mapToHandlerUrl() {
        return new HandlerUrlMapper(thumbnailUrl, channelHandler, subscriberCount);
    }
}
