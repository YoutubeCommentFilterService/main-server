package com.hanhome.youtube_comments.google.object.youtube_data_api.video_category;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
public class VideoCategoryFlatMap {
    private String id;
    private String title;

    public VideoCategoryFlatMap(VideoCategoryResource resource) {
        id = resource.getId();
        title = resource.getSnippet().getTitle();
    }

    @Override
    public String toString() {
        return id + ":" + title;
    }
}
