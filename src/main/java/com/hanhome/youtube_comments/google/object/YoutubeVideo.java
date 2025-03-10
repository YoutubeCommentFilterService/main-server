package com.hanhome.youtube_comments.google.object;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class YoutubeVideo {
    private String id;
    private String title;
    private String thumbnail;
    private LocalDateTime publishedAt;
    private String description;
}
