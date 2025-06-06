package com.hanhome.youtube_comments.mongodb.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@Data
@AllArgsConstructor
@Document(collection = "predicted")
public class PredictedClassDocument {
    @Id
    private String id;
    Set<String> predictedCommentIds;
}
