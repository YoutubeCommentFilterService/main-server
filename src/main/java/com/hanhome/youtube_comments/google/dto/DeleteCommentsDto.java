package com.hanhome.youtube_comments.google.dto;

import lombok.Data;
import org.springframework.lang.Nullable;

public class DeleteCommentsDto {
    @Data
    public static class Request {
        /**
         * 각 field는 comma로 구분된 id 조합
         */
        @Nullable
        private String authorBanComments;

        @Nullable
        private String justDeleteComments;
    }
}
