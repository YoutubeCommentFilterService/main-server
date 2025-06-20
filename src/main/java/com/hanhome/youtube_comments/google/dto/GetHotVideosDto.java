package com.hanhome.youtube_comments.google.dto;

import com.hanhome.youtube_comments.google.object.youtube_data_api.video.VideoFlatMap;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GetHotVideosDto {
    @Getter
    @Builder
    public static class Response implements Serializable {
        private String baseTime;
        private Map<String, List<VideoFlatMap>> itemMap;
        private Set<String> channelIds;
    }
}
