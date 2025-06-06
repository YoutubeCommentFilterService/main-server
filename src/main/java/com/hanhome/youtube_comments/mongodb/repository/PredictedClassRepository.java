package com.hanhome.youtube_comments.mongodb.repository;

import com.hanhome.youtube_comments.mongodb.entity.PredictedClassDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PredictedClassRepository extends MongoRepository<PredictedClassDocument, String> {
}
