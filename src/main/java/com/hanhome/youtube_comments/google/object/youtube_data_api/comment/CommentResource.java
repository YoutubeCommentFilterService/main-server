package com.hanhome.youtube_comments.google.object.youtube_data_api.comment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonIgnoreProperties
@ToString
public class CommentResource {
    private String id;
    private CommentSnippetResource snippet;
}