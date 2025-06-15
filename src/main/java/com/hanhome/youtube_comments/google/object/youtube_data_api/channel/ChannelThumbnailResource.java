package com.hanhome.youtube_comments.google.object.youtube_data_api.channel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChannelThumbnailResource {

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Details {
        private String url;
        private Integer width;
        private Integer height;
    }

    @JsonProperty("default")
    private Details defaultThumbnail;
}
