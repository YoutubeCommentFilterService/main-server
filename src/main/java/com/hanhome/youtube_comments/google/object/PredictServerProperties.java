package com.hanhome.youtube_comments.google.object;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "data.predict-server")
public class PredictServerProperties {
    private String scheme;
    private String host;
    private int port;
}
