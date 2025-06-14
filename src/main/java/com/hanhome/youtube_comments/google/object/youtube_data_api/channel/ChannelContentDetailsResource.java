package com.hanhome.youtube_comments.google.object.youtube_data_api.channel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChannelContentDetailsResource {
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RelatedPlaylists {
        private String uploads;
    }
    private RelatedPlaylists relatedPlaylists;
}
