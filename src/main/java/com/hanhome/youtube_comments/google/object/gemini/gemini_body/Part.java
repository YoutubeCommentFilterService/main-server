package com.hanhome.youtube_comments.google.object.gemini.gemini_body;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Part {
    private String text;
    @JsonProperty(value = "inline_data")
    private InlineData inlineData;
    @JsonProperty(value = "file_data")
    private FileData fileData;

    public Part(String text) {
        this.text = text;
    }
    public Part(InlineData inlineData) {
        this.inlineData = inlineData;
    }
    public Part(FileData fileData) {
        this.fileData = fileData;
    }
}
