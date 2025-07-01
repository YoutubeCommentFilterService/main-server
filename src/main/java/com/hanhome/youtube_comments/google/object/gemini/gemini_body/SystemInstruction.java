package com.hanhome.youtube_comments.google.object.gemini.gemini_body;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemInstruction {
    private List<Part> parts;

    public SystemInstruction() {
        this.parts = new ArrayList<>();
    }

    public SystemInstruction putPart(Part part) {
        this.parts.add(part);
        return this;
    }
}
