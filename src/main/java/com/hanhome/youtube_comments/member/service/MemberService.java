package com.hanhome.youtube_comments.member.service;

import com.hanhome.youtube_comments.google.service.YoutubeDataService;
import com.hanhome.youtube_comments.member.dto.RefreshTokenDto;
import com.hanhome.youtube_comments.member.entity.Member;
import com.hanhome.youtube_comments.member.repository.MemberRepository;
import com.hanhome.youtube_comments.oauth.dto.CustomTokenRecord;
import com.hanhome.youtube_comments.oauth.dto.RenewAccessTokenDto;
import com.hanhome.youtube_comments.oauth.provider.JwtTokenProvider;
import com.hanhome.youtube_comments.redis.service.RedisService;
import com.hanhome.youtube_comments.utils.AESUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Member upsert(OAuth2User oauth2User, String googleAccessToken, String googleRefreshToken) throws Exception {
        String email = oauth2User.getAttribute("email");

        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> {
                    String channelId = youtubeDataService.getChannelId(googleAccessToken);
                    return Member.builder()
                            .channelId(channelId)
                            .email(email)
                            .build();
                });
        String encryptedGoogleRefreshToken = aesUtil.encrypt(googleRefreshToken);
        member.setGoogleRefreshToken(encryptedGoogleRefreshToken);

        return memberRepository.save(member);
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

        return new RefreshTokenDto(refreshToken);
    }

    public CustomTokenRecord renewAccessToken(RenewAccessTokenDto renewDto) throws BadRequestException {
        String refreshToken = renewDto.getRefreshToken();

        try {
            Claims claims = tokenProvider.validate(refreshToken);

            String uuid = claims.getSubject();
            Member member = memberRepository.findById(UUID.fromString(uuid)).orElse(null);
            return tokenProvider.createAccessToken(member.getId(), member.getEmail());
        } catch (Exception e) {
            throw new BadRequestException("유효하지 않은 refreshToken입니다.");
        }
    }

    public void logout(UUID uuid) {
        redisService.searchNRemove(uuid.toString(), false);
    }

    public void withdraw(UUID uuid) {
        memberRepository.deleteById(uuid);
        redisService.searchNRemove(uuid.toString(), false);
    }
}
