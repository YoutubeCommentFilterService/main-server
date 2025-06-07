package com.hanhome.youtube_comments.google.object.youtube_data_api.channel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class ChannelSnippetResource {
    private String title;
    private String customUrl; // handler
    private ChannelThumbnailResource thumbnails;
}
