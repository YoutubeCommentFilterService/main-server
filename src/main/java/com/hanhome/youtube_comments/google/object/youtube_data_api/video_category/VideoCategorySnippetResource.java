package com.hanhome.youtube_comments.google.object.youtube_data_api.video_category;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoCategorySnippetResource {
    private String title;
    private Boolean assignable;
}
