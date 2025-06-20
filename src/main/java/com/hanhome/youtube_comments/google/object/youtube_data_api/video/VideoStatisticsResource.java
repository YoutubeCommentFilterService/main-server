package com.hanhome.youtube_comments.google.object.youtube_data_api.video;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.ToString;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class VideoStatisticsResource {
    public Integer viewCount;
    public Integer likeCount;
    public Integer dislikeCount;
    public Integer commentCount;
}
