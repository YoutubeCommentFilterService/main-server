package com.hanhome.youtube_comments.google.object.youtube_data_api.comment_thread;

import com.hanhome.youtube_comments.google.object.YoutubeComment;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@Builder
@ToString
public class CommentThreadMap {
    private YoutubeComment topLevel;
    private List<YoutubeComment> replies;
}