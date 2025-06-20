package com.hanhome.youtube_comments.google.dto;

import com.hanhome.youtube_comments.google.object.youtube_data_api.video.HotVideoResponseField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.Map;

@AllArgsConstructor
public class GetHotVideosDto {
    @Getter
    @Builder
    public static class Response implements Serializable {
        private String baseTime;
        private Map<String, HotVideoResponseField> itemMap;
    }
}
