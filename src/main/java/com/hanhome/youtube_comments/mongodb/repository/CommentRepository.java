package com.hanhome.youtube_comments.mongodb.repository;

import com.hanhome.youtube_comments.mongodb.entity.CommentDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommentRepository extends MongoRepository<CommentDocument, String> {
}
