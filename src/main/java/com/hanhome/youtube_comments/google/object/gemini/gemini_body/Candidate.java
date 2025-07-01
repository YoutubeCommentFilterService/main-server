package com.hanhome.youtube_comments.google.object.gemini.gemini_body;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Candidate {
    private Content content;
    private String finishReason;
    private Double avgLogprobs;
}
