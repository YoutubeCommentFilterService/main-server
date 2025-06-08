package com.hanhome.youtube_comments.member.entity;

import com.hanhome.youtube_comments.member.object.MemberRole;
import com.hanhome.youtube_comments.member.object.SimpleMemberInfo;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@Table(name = "member", indexes = {
        @Index(name = "idx_channel_id", columnList = "channelId")
})
@Entity
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private String email;

    private String googleRefreshToken;
    private String channelId;
    private String playlistId;
    private String profileImage;
    private String channelName;
    private String channelHandler;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 10)
    private MemberRole role = MemberRole.USER;

    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    private Boolean hasYoutubeAccess = false;

    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    private Boolean isPendingState = false;

    @PrePersist
    public void prePersist() {
        if (channelId == null || channelId.isEmpty()) hasYoutubeAccess = false;
    }

    public SimpleMemberInfo toSimpleInfo() {
        return SimpleMemberInfo.builder()
                .email(email)
                .channelId(channelId)
                .handler(channelHandler)
                .channelName(channelName)
                .imageUrl(profileImage)
                .role(role)
                .userId(id.toString())
                .build();
    }
}