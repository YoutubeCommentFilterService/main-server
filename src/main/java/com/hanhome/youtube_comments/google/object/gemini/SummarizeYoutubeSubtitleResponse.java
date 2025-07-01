package com.hanhome.youtube_comments.google.object.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hanhome.youtube_comments.google.object.gemini.gemini_body.Candidate;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SummarizeYoutubeSubtitleResponse {
    private List<Candidate> candidates;
}
