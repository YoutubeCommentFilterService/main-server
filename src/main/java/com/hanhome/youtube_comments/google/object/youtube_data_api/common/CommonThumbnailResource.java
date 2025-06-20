package com.hanhome.youtube_comments.google.object.youtube_data_api.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Optional;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonThumbnailResource {
    protected Map<String, ThumbnailInfoResource> thumbnails;

    public String getThumbnail() {
        return Optional.ofNullable(thumbnails.get("medium"))
                .map(ThumbnailInfoResource::getUrl)
                .orElse(null);
    }
}
