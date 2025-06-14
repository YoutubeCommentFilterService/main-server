package com.hanhome.youtube_comments.google.object.youtube_data_api.comment_thread;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommentThreadResource {
    private CommentThreadSnippetResource snippet;
    private CommentThreadRepliesResource replies;
}
