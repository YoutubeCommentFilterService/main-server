package com.hanhome.youtube_comments.google.object.youtube_data_api.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.util.Arrays;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class OAuthTokenScopeResponse {
    private String scope;

    public boolean hasYoutubeAccess() {
        return Arrays.stream(scope.split(" "))
                .anyMatch(s -> s.endsWith("youtube.force-ssl"));
    }
}
