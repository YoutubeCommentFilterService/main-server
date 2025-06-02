package com.hanhome.youtube_comments.google.object.predict;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PredictionResponse {
    private List<PredictionResultResource> items;

    @JsonProperty("nickname_categories")
    private List<String> nicknameCategories;

    @JsonProperty("comment_categories")
    private List<String> commentCategories;
}
