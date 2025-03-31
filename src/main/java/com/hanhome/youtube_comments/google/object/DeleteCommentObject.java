package com.hanhome.youtube_comments.google.object;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DeleteCommentObject {
    private String commentId;
    private String channelId;
}
