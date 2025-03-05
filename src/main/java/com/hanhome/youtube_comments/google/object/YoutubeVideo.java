package com.hanhome.youtube_comments.google.object;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder

public class YoutubeVideo {
    private String id;
    private String title;
    private String thumbnail;
    private LocalDateTime publishedAt;
    private String description;
}
