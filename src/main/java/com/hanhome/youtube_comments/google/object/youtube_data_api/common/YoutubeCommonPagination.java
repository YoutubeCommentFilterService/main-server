package com.hanhome.youtube_comments.google.object.youtube_data_api.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class YoutubeCommonPagination {
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @ToString
    public static class PageInfo {
        private Integer totalResults;
        private Integer resultsPerPage;
    }
    private String nextPageToken;
    private PageInfo pageInfo;

    public Integer getTotalResults() { return pageInfo.getTotalResults(); }

    public YoutubeCommonPagination() {
        nextPageToken = null;
    }
}
