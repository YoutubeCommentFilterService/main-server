package com.hanhome.youtube_comments.member.service;

import com.hanhome.youtube_comments.google.exception.YoutubeAccessForbiddenException;
import com.hanhome.youtube_comments.google.service.GoogleAPIService;
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
import java.util.Optional;
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
    private final GoogleAPIService googleAPIService;

    @Value("${data.youtube.access-token}")
    private String redisGoogleAtKey;

    public Boolean checkMemberIsNew(String email) {
        Optional<Member> memberOpt = memberRepository.findByEmail(email);
        if (memberOpt.isPresent()) return memberOpt.get().getIsPendingState();
        return true;
    }

    @Transactional
    public void rejectSignup(String email) throws Exception {
        Optional<Member> memberOpt = memberRepository.findByEmail(email);
        if (memberOpt.isPresent()) {
            UUID uuid = memberOpt.get().getId();
            revokeGoogleGrant(uuid);
            memberRepository.delete(memberOpt.get());
        }

    }

    @Transactional
    public void acceptSignup(String email) {
        Member member = memberRepository.findByEmail(email).get();
        member.setIsPendingState(false);
    }

    @Transactional
    public Member insert(String email) {
        Optional<Member> member = memberRepository.findByEmail(email);
        if (member.isPresent()) {
            member.get().setIsPendingState(false);
            return member.get();
        }
        return null;
    }

    @Transactional
    public Member upsert(OAuth2User oauth2User, String googleAccessToken, String googleRefreshToken) throws Exception {
        String email = oauth2User.getAttribute("email");
        String profileImageUrl = oauth2User.getAttribute("picture");
        String youtubeChannelName = oauth2User.getAttribute("name");
        String encryptedGoogleRefreshToken = aesUtil.encrypt(googleRefreshToken);

        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> {
                    LocalDateTime createdAt = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
                    Member newMember = Member.builder()
                            .email(email)
                            .googleRefreshToken(encryptedGoogleRefreshToken)
                            .channelName(youtubeChannelName)
                            .profileImage(profileImageUrl)
                            .isPendingState(true)
                            .createdAt(createdAt)
                            .role(MemberRole.UNLINKED)
                            .hasYoutubeAccess(false)
                            .build();

                    return memberRepository.save(newMember);
                });

        memberRepository.save(member);

        if (!member.getHasYoutubeAccess()) {
            assignYoutubeAccountDetail(member, googleAccessToken);
        }

        return member;
    }

    private void assignYoutubeAccountDetail(Member member, String googleAccessToken) {
        try {
            YoutubeAccountDetail detail = youtubeDataService.getYoutubeAccountDetail(googleAccessToken);
            boolean hasYoutubeAccess = googleAPIService.hasYoutubeAccess(googleAccessToken);

            member.setChannelId(detail.getChannelId());
            member.setPlaylistId(detail.getPlaylistId());
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

    private void revokeGoogleGrant(UUID uuid) throws Exception {
        String googleAccessToken = (String) redisService.get(redisGoogleAtKey + ":" + uuid);

        googleAccessToken = googleAccessToken == null ? googleAPIService.takeNewGoogleAccessToken(uuid) : googleAccessToken;

        String revokeUrl = "https://oauth2.googleapis.com/revoke?token=" + googleAccessToken;
        new RestTemplate().postForObject(revokeUrl, null, String.class);
    }

    public void withdraw(UUID uuid) throws Exception {
        revokeGoogleGrant(uuid);

        memberRepository.deleteById(uuid);
        redisService.searchNRemove(uuid.toString(), false);
    }
}
