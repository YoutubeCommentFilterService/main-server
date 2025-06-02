package com.hanhome.youtube_comments.google.object.youtube_data_api.playlist_items;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class PlaylistItemResource {

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @ToString
    public static class Status {
        private String privacyStatus;
    }
    private String id;
    private PlaylistItemSnippetResource snippet;
    private Status status;

    public String getPrivacyStatus() { return status.getPrivacyStatus(); }
}
