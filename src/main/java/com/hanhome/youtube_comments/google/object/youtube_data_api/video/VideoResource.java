package com.hanhome.youtube_comments.google.object.youtube_data_api.video;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoResource {
    private String id;
    private VideoSnippetResource snippet;
    private VideoStatisticsResource statistics;
}
