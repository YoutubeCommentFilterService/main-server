package com.hanhome.youtube_comments.member.dao;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@Builder
public class MemberChannelIdDAO implements Serializable {
    private UUID id;
    private String channelId;
}
