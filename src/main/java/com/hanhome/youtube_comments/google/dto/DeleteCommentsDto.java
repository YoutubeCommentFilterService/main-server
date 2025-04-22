package com.hanhome.youtube_comments.google.dto;

import com.hanhome.youtube_comments.google.object.DeleteCommentObject;
import lombok.Data;

import java.util.List;

public class DeleteCommentsDto {
    @Data
    public static class Request {
        private String videoId;
        private List<DeleteCommentObject> authorBanComments;
        private List<DeleteCommentObject> justDeleteComments;
    }
}
