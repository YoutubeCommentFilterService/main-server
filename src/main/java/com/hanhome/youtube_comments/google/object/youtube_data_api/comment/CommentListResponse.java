package com.hanhome.youtube_comments.google.object.youtube_data_api.comment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hanhome.youtube_comments.google.object.youtube_data_api.common.YoutubeCommonPagination;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommentListResponse extends YoutubeCommonPagination {
    private List<CommentResource> items;
}
