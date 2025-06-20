package com.hanhome.youtube_comments.google.object.youtube_data_api.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThumbnailInfoResource {
    private String url;
    private Integer width;
    private Integer height;
}
