package com.hanhome.youtube_comments.mongodb.repository;

import com.hanhome.youtube_comments.mongodb.entity.NicknameDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NicknameRepository extends MongoRepository<NicknameDocument, String> {
}
