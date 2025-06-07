package com.hanhome.youtube_comments.member.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanhome.youtube_comments.google.exception.YoutubeAccessForbiddenException;
import com.hanhome.youtube_comments.google.service.RenewGoogleTokenService;
import com.hanhome.youtube_comments.google.service.YoutubeDataService;
import com.hanhome.youtube_comments.member.dto.AccessTokenDto;
import com.hanhome.youtube_comments.member.dto.RefreshTokenDto;
import com.hanhome.youtube_comments.member.entity.Member;
import com.hanhome.youtube_comments.member.object.MemberRole;
import com.hanhome.youtube_comments.member.object.YoutubeAccountDetail;
import com.hanhome.youtube_comments.member.repository.MemberRepository;
import com.hanhome.youtube_comments.oauth.dto.CustomTokenRecord;
import com.hanhome.youtube_comments.oauth.dto.RenewAccessTokenDto;
import com.hanhome.youtube_comments.oauth.provider.JwtTokenProvider;
import com.hanhome.youtube_comments.redis.service.RedisService;
import com.hanhome.youtube_comments.utils.AESUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtTokenProvider tokenProvider;
    private final RedisService redisService;
    private final YoutubeDataService youtubeDataService;
    private final AESUtil aesUtil;
    private final RenewGoogleTokenService googleTokenService;
    private final ObjectMapper objectMapper;

    @Value("${data.youtube.access-token}")
    private String redisGoogleAtKey;

    private final static String pendingSignupKey = "PENDING_SIGNUP";

    public Boolean checkMemberIsNew(String email) {
        Object redisVal = redisService.get(pendingSignupKey + ":" + email);
        return redisVal != null;
    }

    public void clearRedisEmailKey(String email) {
        redisService.remove(pendingSignupKey + ":" + email);
    }

    @Transactional
    public Member insert(String email) {
        Member member = objectMapper.convertValue(redisService.get(pendingSignupKey + ":" + email), Member.class);
        if (member != null) {
            member.setIsNewMember(false);
            return memberRepository.save(member);
        }
        return null;
    }

    @Transactional
    public Member upsert(OAuth2User oauth2User, String googleAccessToken, String googleRefreshToken) throws Exception {
        String email = oauth2User.getAttribute("email");
        String encryptedGoogleRefreshToken = aesUtil.encrypt(googleRefreshToken);
        boolean hasYoutubeAccess = youtubeDataService.hasYoutubeAccess(googleAccessToken);

        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> {
                    LocalDateTime createdAt = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
                    Member newMember = Member.builder()
                                    .email(email)
                                    .googleRefreshToken(encryptedGoogleRefreshToken)

                                    .isNewMember(true)
                                    .createdAt(createdAt)
                                    .role(MemberRole.UNLINKED)
                                    .build();

                    assignYoutubeAccountDetail(newMember, googleAccessToken, hasYoutubeAccess);
                    return newMember;
                });

        if (!member.getHasYoutubeAccess() && hasYoutubeAccess) {
            assignYoutubeAccountDetail(member, googleAccessToken, hasYoutubeAccess);
        }

        if (!member.getIsNewMember()) member = memberRepository.save(member);
        else redisService.save(pendingSignupKey + ":" + email, member, 60 * 5);

        return member;
    }

    private void assignYoutubeAccountDetail(Member member, String googleAccessToken, boolean hasYoutubeAccess) {
        try {
            YoutubeAccountDetail detail = youtubeDataService.getYoutubeAccountDetail(googleAccessToken);

            member.setChannelId(detail.getChannelId());
            member.setPlaylistId(detail.getPlaylistId());
            member.setProfileImage(detail.getThumbnailUrl());
            member.setChannelName(detail.getChannelName());
            member.setChannelHandler(detail.getChannelHandler());
            member.setRole(MemberRole.USER);
            member.setHasYoutubeAccess(hasYoutubeAccess);

        } catch (YoutubeAccessForbiddenException ignored) {}
    }

    public RefreshTokenDto getRefreshToken(UUID uuid) {
        Member member = memberRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("회원가입을 먼저 해주세요"));

        String refreshToken = (String) redisService.get("REFRESH:" + uuid.toString());
        if (refreshToken == null) {
            CustomTokenRecord customToken = tokenProvider.createRefreshToken(uuid);
            refreshToken = customToken.token();
            long ttl = customToken.ttl();
            TimeUnit timeUnit = customToken.timeUnit();
            redisService.save("REFRESH:" + uuid.toString(), refreshToken, ttl, timeUnit);
        }

        return RefreshTokenDto.builder()
                .refreshToken(refreshToken)
                .nickname(member.getChannelName())
                .profileImage(member.getProfileImage())
                .hasYoutubeAccess(member.getHasYoutubeAccess())
                .role(member.getRole())
                .build();
    }

    public AccessTokenDto.Renew renewAccessToken(RenewAccessTokenDto renewDto) throws BadRequestException {
        String refreshToken = renewDto.getRefreshToken();

        try {
            Claims claims = tokenProvider.validate(refreshToken);

            String uuid = claims.getSubject();
            Member member = memberRepository.findById(UUID.fromString(uuid)).orElse(null);
            return AccessTokenDto.Renew.builder()
                    .customTokenRecord(tokenProvider.createAccessToken(member.getId(), member.getEmail()))
                    .nickname(member.getChannelName())
                    .profileImage(member.getProfileImage())
                    .hasYoutubeAccess(member.getHasYoutubeAccess())
                    .role(member.getRole().name())
                    .build();
        } catch (Exception e) {
            throw new BadRequestException("유효하지 않은 refreshToken입니다.");
        }
    }

    public void logout(UUID uuid) {
        redisService.searchNRemove(uuid.toString(), false);
    }

    public void withdraw(UUID uuid) throws Exception {
        String googleAccessToken = getGoogleAccessToken(uuid);
        String revokeUrl = "https://oauth2.googleapis.com/revoke?token=" + googleAccessToken;
        revokeGoogleOAuth(revokeUrl);

        memberRepository.deleteById(uuid);
        redisService.searchNRemove(uuid.toString(), false);
    }

    public void revokeGoogleOAuth(String revokeUrl) {
        new RestTemplate().postForObject(revokeUrl, null, String.class);
    }

    private String getGoogleAccessToken(UUID uuid) throws Exception {
        String googleAccessToken = (String) redisService.get(redisGoogleAtKey + ":" + uuid);
        return googleAccessToken == null ? googleTokenService.renewAccessToken(uuid) : googleAccessToken;
    }
}
