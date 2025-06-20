package com.hanhome.youtube_comments.google.object.youtube_data_api.video;

import com.hanhome.youtube_comments.member.object.YoutubeAccountDetail;
import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
public class VideoFlatMap implements Serializable {
    private final String videoId;
    private final String title;
    private final String description;
    private final String tags;
    private final String publishedAt;
    private final String thumbnailUrl;
    private final String viewCount;
    private final String likeCount;
    private final String dislikeCount;
    private final String commentCount;

    private final String channelId;
    private final String channelTitle;
    private String channelHandler;
    private String channelThumbnailUrl = "";
    private String subscriberCount;

    public VideoFlatMap(VideoResource resource) {
        VideoSnippetResource snippet = resource.getSnippet();
        VideoStatisticsResource statistics = resource.getStatistics();
        videoId = resource.getId();
        title = replaceSpacer(snippet.getLocalized().getTitle());
        description = replaceSpacer(snippet.getLocalized().getDescription());
        publishedAt = snippet.getPublishedAt();
        thumbnailUrl = snippet.getThumbnail();

        viewCount = convertVideoStatisticFormat(statistics.getViewCount(), "뷰");
        likeCount = convertVideoStatisticFormat(statistics.getLikeCount(), "");
        dislikeCount = convertVideoStatisticFormat(statistics.getDislikeCount(), "");
        commentCount = convertVideoStatisticFormat(statistics.getCommentCount(), "개");

        channelTitle = snippet.getChannelTitle().replaceAll("#[가-힣a-zA-Z0-9]", "").trim();
        channelId = snippet.getChannelId();
        List<String> tags = snippet.getTags() != null
                ? snippet.getTags().stream().map(tag -> "#"+tag).toList()
                : new ArrayList<>();
        tags = tags.subList(0, Math.min(tags.size() , 20));
        this.tags = String.join(" ", tags);
        channelThumbnailUrl = "";
        channelHandler = "";
        subscriberCount = "";
    }

    private String replaceSpacer(String str) {
        return str.replaceAll("[\t\r\n]", "");
    }

    public void setNonstaticField(YoutubeAccountDetail.HandlerUrlMapper obj) {
        this.channelHandler = obj.getChannelHandler();
        this.channelThumbnailUrl = obj.getThumbnailUrl();
        this.subscriberCount = convertVideoStatisticFormat(obj.getSubscriberCount(), "");
    }

    private String convertVideoStatisticFormat(Integer statistic, String type) {
        if (statistic == null) return "NONE";
        if (statistic >= 100_000_000) {
            return String.format("%.2f억%s", statistic / 100_000_000.0, type);
        } else if (statistic >= 10_000) {
            return String.format("%.2f만%s", statistic / 10_000.0, type);
        } else if (statistic >= 1_000) {
            return String.format("%.2f천%s", statistic / 1_000.0, type);
        }
        return statistic + type;
    }
}
