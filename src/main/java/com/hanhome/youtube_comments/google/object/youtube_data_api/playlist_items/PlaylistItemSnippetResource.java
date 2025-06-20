package com.hanhome.youtube_comments.google.object.youtube_data_api.playlist_items;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hanhome.youtube_comments.google.object.youtube_data_api.common.CommonThumbnailResource;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaylistItemSnippetResource extends CommonThumbnailResource {
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
    private ResourceId resourceId;

    public String getVideoId() { return resourceId.getVideoId(); }
}
