package com.hanhome.youtube_comments.common.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PredictCommonResponse {
    private Integer code;
    private String message;
}
