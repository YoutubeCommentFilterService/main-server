package com.hanhome.youtube_comments.google.object.youtube_data_api.comment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommentSnippetResource {
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthorChannelId {
        private String value;
    }

    private String authorDisplayName;
    private String authorProfileImageUrl;
    private String textOriginal;
    private AuthorChannelId authorChannelId; // TODO: 왜 authorChannelId를 넣었는가? 고찰 <- 채널 ban의 경우 필요

    public String getAuthorChannelId() { return authorChannelId.getValue(); }
}
