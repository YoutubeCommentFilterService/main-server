package com.hanhome.youtube_comments.member.object;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class SimpleMemberInfo {
    private String handler;
    private String channelId;
    private String email;
    private String imageUrl;
    private String channelName;
    private MemberRole role;
    private String userId;
}
