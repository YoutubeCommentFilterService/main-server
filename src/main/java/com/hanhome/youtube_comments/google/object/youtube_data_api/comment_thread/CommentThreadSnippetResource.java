package com.hanhome.youtube_comments.google.object.youtube_data_api.comment_thread;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hanhome.youtube_comments.google.object.youtube_data_api.comment.CommentResource;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class CommentThreadSnippetResource {
    private Integer totalReplyCount;
    private CommentResource topLevelComment;
}
