package com.hanhome.youtube_comments.google.object.youtube_data_api.comment_thread;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hanhome.youtube_comments.google.object.youtube_data_api.common.YoutubeCommonPagination;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class CommentThreadListResponse extends YoutubeCommonPagination {
    private List<CommentThreadResource> items;
}
