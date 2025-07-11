package com.hanhome.youtube_comments.google.object.youtube_data_api.comment_thread;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hanhome.youtube_comments.google.object.youtube_data_api.common.CommonPagination;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommentThreadListResponse extends CommonPagination {
    private List<CommentThreadResource> items;
}
