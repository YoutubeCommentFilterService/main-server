package com.hanhome.youtube_comments.mongodb.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@AllArgsConstructor
@Document(collection = "comments")
@CompoundIndexes({
        @CompoundIndex(name = "channelId_1", def = "{ 'channelId': 1 }"),
        @CompoundIndex(name = "videoId_1", def = "{ 'videoId': 1 }")
})
public class CommentDocument {
    @Id
    private String id;
    private String channelId;
    private String videoId;
    private String predicted;
    private List<Float> prob;
    private String text;
//    private String updatedAt;
}
