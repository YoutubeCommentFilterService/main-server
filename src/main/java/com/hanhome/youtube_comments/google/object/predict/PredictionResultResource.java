package com.hanhome.youtube_comments.google.object.predict;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PredictionResultResource {
    @JsonProperty("nickname_predicted")
    private String nicknamePredicted;
    @JsonProperty("comment_predicted")
    private String commentPredicted;
    @JsonProperty("nickname_predicted_prob")
    private List<Float> nicknamePredictProbs;
    @JsonProperty("comment_predicted_prob")
    private List<Float> commentPredictProbs;

}
