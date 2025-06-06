package com.hanhome.youtube_comments.mongodb.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Objects;

@Data
@AllArgsConstructor
@Document(collection = "nicknames")
@CompoundIndexes({
        @CompoundIndex(name = "text_1", def = "{ 'text': 1 }"),
})
public class NicknameDocument {
    @Id
    private String channelId;
    private String text;
    private String predicted;
    private List<Float> prob;
    private String profileImgUrl;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NicknameDocument that)) return false;
        return Objects.equals(channelId, that.getChannelId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId);
    }
}