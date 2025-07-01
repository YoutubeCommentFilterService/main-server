package com.hanhome.youtube_comments.google.object.gemini;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hanhome.youtube_comments.google.object.gemini.gemini_body.Content;
import com.hanhome.youtube_comments.google.object.gemini.gemini_body.GenerationConfig;
import com.hanhome.youtube_comments.google.object.gemini.gemini_body.Part;
import com.hanhome.youtube_comments.google.object.gemini.gemini_body.SystemInstruction;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SummarizeYoutubeSubtitleRequest {
    @JsonProperty(value = "system_instruction")
    private SystemInstruction systemInstruction;
    private List<Content> contents;
    private GenerationConfig generationConfig;

    public SummarizeYoutubeSubtitleRequest() {
        this.systemInstruction = new SystemInstruction();
        this.contents = new ArrayList<>();
    }

    public SummarizeYoutubeSubtitleRequest putContentPart(Part part) {
        if (this.contents.isEmpty()) {
            Content content = new Content();
            this.contents.add(content);
        }
        this.contents.get(0).getParts().add(part);
        return this;
    }
}