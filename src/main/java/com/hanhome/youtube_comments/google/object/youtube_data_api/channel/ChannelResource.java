package com.hanhome.youtube_comments.google.object.youtube_data_api.channel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChannelResource {
    private String id;
    private ChannelSnippetResource snippet;
    private ChannelContentDetailsResource contentDetails;
    private ChannelStatisticsResource statistics;
}
