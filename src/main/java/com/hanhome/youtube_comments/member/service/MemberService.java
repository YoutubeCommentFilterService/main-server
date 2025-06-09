package com.hanhome.youtube_comments.member.service;

import com.hanhome.youtube_comments.exception.GoogleRefreshTokenNotFoundException;
import com.hanhome.youtube_comments.exception.RenewAccessTokenFailedException;
import com.hanhome.youtube_comments.exception.YoutubeAccessForbiddenException;
import com.hanhome.youtube_comments.google.service.GoogleAPIService;
import com.hanhome.youtube_comments.google.service.YoutubeDataService;
import com.hanhome.youtube_comments.member.dto.AccessTokenDto;
import com.hanhome.youtube_comments.member.dto.IsNewMemberDto;
import com.hanhome.youtube_comments.member.dto.RefreshTokenDto;
import com.hanhome.youtube_comments.member.entity.Member;
import com.hanhome.youtube_comments.exception.InvalidJWTTokenException;
import com.hanhome.youtube_comments.exception.UserNotFountException;
import com.hanhome.youtube_comments.member.object.MemberRole;
import com.hanhome.youtube_comments.member.object.YoutubeAccountDetail;
import com.hanhome.youtube_comments.member.repository.MemberRepository;
import com.hanhome.youtube_comments.oauth.dto.CustomTokenRecord;
import com.hanhome.youtube_comments.oauth.dto.RenewAccessTokenDto;
import com.hanhome.youtube_comments.oauth.provider.JwtTokenProvider;
import com.hanhome.youtube_comments.oauth.service.CookieService;
import com.hanhome.youtube_comments.redis.service.RedisService;
import com.hanhome.youtube_comments.utils.AESUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
    private final CookieService cookieService;

    @Value("${data.youtube.access-token}")
    private String redisGoogleAtKey;

    @Value("${spring.app.default-domain}")
    private String defaultDomain;

    public IsNewMemberDto.Response checkIsNewMemberFromCookie(Cookie[] cookies) {
        String email = getSpecificCookieVal(cookies, "email");
        String accessToken = getSpecificCookieVal(cookies, "access_token");
        if (!"".equals(email)) {
            boolean isNewMember = checkIsNewMember(email);
            return IsNewMemberDto.Response.builder()
                    .isNewMember(isNewMember)
                    .build();
        } else if (!"".equals(accessToken)) {
            return IsNewMemberDto.Response.builder()
                    .isNewMember(false)
                    .build();
        }
        return null;
    }

    private String getSpecificCookieVal(Cookie[] cookies, String cookieKey) {
        for (Cookie cookie : cookies) {
            if (cookieKey.equals(cookie.getName())) return cookie.getValue();
        }
        return "";
    }

    private String getAndRemoveSpecificCookie(HttpServletResponse response, Cookie[] cookies, String cookieKey) {
        String cookieVal = getSpecificCookieVal(cookies, cookieKey);
        Cookie cookie = new Cookie(cookieKey, null);
        cookie.setDomain(defaultDomain);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return cookieVal;
    }

    public Boolean checkIsNewMember(String email) {
        Optional<Member> memberOpt = memberRepository.findByEmail(email);
        if (memberOpt.isPresent()) return memberOpt.get().getIsPendingState();
        return true;
    }

    public void rejectSignup(Cookie[] cookies, HttpServletResponse response) {
        String email = getAndRemoveSpecificCookie(response, cookies, "email");
        if (email != null && !email.isEmpty()) rejectSignup(email);
    }

    @Transactional
    public void rejectSignup(String email) {
        Optional<Member> memberOpt = memberRepository.findByEmail(email);
        if (memberOpt.isPresent()) {
            UUID uuid = memberOpt.get().getId();
            revokeGoogleGrant(uuid);
            memberRepository.delete(memberOpt.get());
        }
    }

    public boolean acceptSignup(Cookie[] cookies, HttpServletResponse response) {
        String email = getAndRemoveSpecificCookie(response, cookies, "email");
        if ("".equals(email)) return false;

        Member member = updatePendingState(email);
        if (member == null) return false;

        CustomTokenRecord customAccessToken = tokenProvider.createAccessToken(member.getId(), member.getEmail());
        long ttl = customAccessToken.ttl();
        TimeUnit timeUnit = customAccessToken.timeUnit();

        Cookie accessTokenCookie = cookieService.getAccessTokenCookie(customAccessToken.token(), (int) timeUnit.toSeconds(ttl));
        response.addCookie(accessTokenCookie);
        return true;
    }

    public Member updatePendingState(String email) {
        return updatePendingState(email, false);
    }

    @Transactional
    public Member updatePendingState (String email, boolean pendingState) {
        Optional<Member> member = memberRepository.findByEmail(email);
        if (member.isPresent()) {
            member.get().setIsPendingState(pendingState);
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

    public AccessTokenDto.Response renewAccessToken(RenewAccessTokenDto renewDto, HttpServletResponse response) {
        String refreshToken = renewDto.getRefreshToken();

        String uuid = null;
        try {
            Claims claims = tokenProvider.validate(refreshToken);

            uuid = claims.getSubject();
            System.out.println(uuid);
            AccessTokenDto.Renew renewedAccessToken = getRenewedAccessToken(uuid);

            CustomTokenRecord customToken = renewedAccessToken.getCustomTokenRecord();
            String token = customToken.token();
            long ttl = customToken.ttl();
            TimeUnit timeUnit = customToken.timeUnit();

            Cookie tokenCookie = cookieService.getAccessTokenCookie(token, ((int) timeUnit.toSeconds(ttl)));
            response.addCookie(tokenCookie);

            return AccessTokenDto.Response.builder()
                    .profileImage(renewedAccessToken.getProfileImage())
                    .nickname(renewedAccessToken.getNickname())
                    .hasYoutubeAccess(renewedAccessToken.getHasYoutubeAccess())
                    .role(renewedAccessToken.getRole())
                    .build();
        } catch (InvalidJWTTokenException e) {
            throw new InvalidJWTTokenException("유효하지 않은 Refresh Token입니다: " + e.getMessage());
        } catch (UserNotFountException e) {
            throw new UserNotFountException("유저가 없습니다:" + uuid);
        }
    }

    private AccessTokenDto.Renew getRenewedAccessToken(String uuid) {
        Member member = memberRepository.findById(UUID.fromString(uuid))
                .orElseThrow(() -> new UserNotFountException("User Not Found!"));
        return AccessTokenDto.Renew.builder()
                .customTokenRecord(tokenProvider.createAccessToken(member.getId(), member.getEmail()))
                .nickname(member.getChannelName())
                .profileImage(member.getProfileImage())
                .hasYoutubeAccess(member.getHasYoutubeAccess())
                .role(member.getRole().name())
                .build();
    }

    public void logout(UUID uuid) {
        redisService.searchNRemove(uuid.toString(), false);
    }

    private void revokeGoogleGrant(UUID uuid) {
        try {
            String googleAccessToken = (String) redisService.get(redisGoogleAtKey + ":" + uuid);

            googleAccessToken = googleAccessToken == null ? googleAPIService.takeNewGoogleAccessToken(uuid) : googleAccessToken;

            String revokeUrl = "https://oauth2.googleapis.com/revoke?token=" + googleAccessToken;
            new RestTemplate().postForObject(revokeUrl, null, String.class);
        } catch (RenewAccessTokenFailedException | GoogleRefreshTokenNotFoundException e) {
            System.out.println("Revoke failed: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unknown Error: " + e);
        }
    }

    public void withdraw(UUID uuid) {
        revokeGoogleGrant(uuid);

        memberRepository.deleteById(uuid);
        redisService.searchNRemove(uuid.toString(), false);
    }
}
