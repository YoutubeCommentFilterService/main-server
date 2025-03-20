package com.hanhome.youtube_comments.common.errors;

import com.fasterxml.jackson.databind.JsonNode;

public class CustomPredictServerException extends RuntimeException {
    private final JsonNode errorBody;

    public CustomPredictServerException(String message, JsonNode errorBody) {
        super(message);
        this.errorBody = errorBody;
    }

    public JsonNode getErrorBody() {
        return errorBody;
    }
}
