package com.hanhome.youtube_comments.google.object.youtube_data_api.playlist_items;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaylistItemSnippetResource {
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ThumbnailInfo {
        private String url;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResourceId {
        private String videoId;
    }

    private ZonedDateTime publishedAt;
    private String title;
    private String description;
    private Map<String, ThumbnailInfo> thumbnails;
    private ResourceId resourceId;

    public String getVideoId() { return resourceId.getVideoId(); }
    public String getThumbnail() {
        return Optional.ofNullable(thumbnails.get("medium"))
                .map(ThumbnailInfo::getUrl)
                .orElse(null);
    }
}
