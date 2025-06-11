package com.hanhome.youtube_comments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableMongoRepositories
@EnableCaching
public class YoutubeCommentsApplication {

	public static void main(String[] args) {
		SpringApplication.run(YoutubeCommentsApplication.class, args);
	}

}
