package com.hanhome.youtube_comments.google.object.youtube_data_api.comment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties
public class CommentResource {
    private String id;
    private CommentSnippetResource snippet;
}