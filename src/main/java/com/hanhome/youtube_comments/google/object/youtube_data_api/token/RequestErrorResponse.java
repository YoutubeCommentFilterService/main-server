package com.hanhome.youtube_comments.google.object.youtube_data_api.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class RequestErrorResponse {
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @ToString
    public static class ErrorCode {
        @Getter
        @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        @ToString
        public static class ErrorStatus {
            private String message;
        }

        private Integer code;
        private String status;
        private List<ErrorStatus> errors;
    }

    private ErrorCode error;
}
