package com.hanhome.youtube_comments.google.object;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubeVideo {
    private String id;
    private String title;
    private String thumbnail;
    private LocalDateTime publishedAt;
    private String description;
    private String privacy;
}
