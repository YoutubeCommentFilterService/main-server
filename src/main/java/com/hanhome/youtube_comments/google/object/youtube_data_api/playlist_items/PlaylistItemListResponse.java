package com.hanhome.youtube_comments.google.object.youtube_data_api.playlist_items;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hanhome.youtube_comments.google.object.youtube_data_api.common.YoutubeCommonPagination;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
public class PlaylistItemListResponse extends YoutubeCommonPagination {
    private List<PlaylistItemResource> items;
}
