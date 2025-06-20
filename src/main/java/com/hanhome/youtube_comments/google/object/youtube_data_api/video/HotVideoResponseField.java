package com.hanhome.youtube_comments.google.object.youtube_data_api.video;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor
public class HotVideoResponseField {
    private Integer key;
    private List<VideoFlatMap> items;
}
