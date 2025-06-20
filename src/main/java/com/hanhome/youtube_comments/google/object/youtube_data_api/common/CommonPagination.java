package com.hanhome.youtube_comments.google.object.youtube_data_api.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonPagination {
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageInfo {
        private Integer totalResults;
        private Integer resultsPerPage;
    }
    protected String nextPageToken;
    protected PageInfo pageInfo;

    public Integer getTotalResults() { return pageInfo.getTotalResults(); }

    public CommonPagination() {
        nextPageToken = null;
    }
}
