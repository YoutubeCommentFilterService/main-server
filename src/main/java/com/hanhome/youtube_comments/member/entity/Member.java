package com.hanhome.youtube_comments.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@Table(name = "member")
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private String email;

    private String googleRefreshToken;
    private String refreshToken;
    private String channelId;
    private String playlistId;
    private String profileImage;
    private String nickname;

    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public void setGoogleRefreshToken(String refreshToken) { this.googleRefreshToken = refreshToken; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}
