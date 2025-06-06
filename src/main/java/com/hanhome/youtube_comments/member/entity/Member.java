package com.hanhome.youtube_comments.member.entity;

import com.hanhome.youtube_comments.member.object.MemberRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@Table(name = "member")
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
    private String siteRefreshToken;
    private String channelId;
    private String playlistId;
    private String profileImage;
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 10)
    private MemberRole role = MemberRole.USER;

    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    private Boolean hasYoutubeAccess = false;

    private Boolean isNewMember;

    @PrePersist
    public void prePersist() {
        if (channelId == null || channelId.isEmpty()) hasYoutubeAccess = false;
    }
}