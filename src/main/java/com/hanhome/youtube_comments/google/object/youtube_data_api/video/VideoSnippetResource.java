package com.hanhome.youtube_comments.google.object.youtube_data_api.video;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hanhome.youtube_comments.google.object.youtube_data_api.common.CommonThumbnailResource;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoSnippetResource extends CommonThumbnailResource {
    private String publishedAt;
    private String channelId;
    private String title;
    private String description;
    private String categoryId;
    private String channelTitle;
    private List<String> tags;
    private Localized localized;

    @Getter
    @Setter
    public static class Localized {
        private String title;
        private String description;
    }
}
