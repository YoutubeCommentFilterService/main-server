package com.hanhome.youtube_comments.google.object.youtube_data_api.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestErrorResponse {
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorCode {
        @Getter
        @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ErrorStatus {
            private String message;
            private String reason;

            public ErrorStatus() {
                message = "UNKNOWN";
                reason = "UNKNOWN";
            }
        }

        private Integer code;
        private String status;
        private List<ErrorStatus> errors;
    }

    private ErrorCode error;
}
