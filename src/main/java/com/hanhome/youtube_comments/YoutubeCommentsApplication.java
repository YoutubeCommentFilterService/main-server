package com.hanhome.youtube_comments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class YoutubeCommentsApplication {

	public static void main(String[] args) {
		SpringApplication.run(YoutubeCommentsApplication.class, args);
	}

}
