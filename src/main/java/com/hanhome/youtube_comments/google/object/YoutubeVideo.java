package com.hanhome.youtube_comments.google.object;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder

public class YoutubeVideo {
    private String id;
    private String title;
    private String thumbnail;
    private LocalDate publishedAt;
    private String description;
}
