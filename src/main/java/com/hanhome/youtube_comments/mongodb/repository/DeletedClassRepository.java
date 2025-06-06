package com.hanhome.youtube_comments.mongodb.repository;

import com.hanhome.youtube_comments.mongodb.entity.DeletedClassDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DeletedClassRepository extends MongoRepository<DeletedClassDocument, String> {
}
