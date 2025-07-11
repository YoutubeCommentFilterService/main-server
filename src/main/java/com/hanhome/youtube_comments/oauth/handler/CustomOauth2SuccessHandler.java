package com.hanhome.youtube_comments.oauth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanhome.youtube_comments.member.entity.Member;
import com.hanhome.youtube_comments.member.service.MemberService;
import com.hanhome.youtube_comments.oauth.dto.CustomTokenRecord;
import com.hanhome.youtube_comments.oauth.provider.JwtTokenProvider;
import com.hanhome.youtube_comments.oauth.service.CookieService;
import com.hanhome.youtube_comments.redis.service.RedisService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class CustomOauth2SuccessHandler implements AuthenticationSuccessHandler {
    private final OAuth2AuthorizedClientRepository authorizedClientRepository;
    private final RedisService redisService;
    private final MemberService memberService;
    private final JwtTokenProvider tokenProvider;
    private final CookieService cookieService;

    @Value("${data.youtube.access-token}")
    private String redisGoogleAtKey;

    @Value("${spring.app.redirect-url}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauth2Authentication = (OAuth2AuthenticationToken) authentication;

        OAuth2AuthorizedClient authorizedClient = authorizedClientRepository.loadAuthorizedClient(
                oauth2Authentication.getAuthorizedClientRegistrationId(),
                oauth2Authentication,
                request
        );
        String googleAccessToken = "";
        String googleRefreshToken = "";

        if (authorizedClient != null) {
            OAuth2AccessToken oauthAccessToken = authorizedClient.getAccessToken();
            OAuth2RefreshToken oauthRefreshToken = authorizedClient.getRefreshToken();
            googleAccessToken = oauthAccessToken.getTokenValue();
            if (oauthRefreshToken != null) {
                googleRefreshToken = oauthRefreshToken.getTokenValue();
            }
        }

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        try {
            Member member = memberService.upsert(oauthUser, googleAccessToken, googleRefreshToken);

            if (member.getIsPendingState()) {
                Cookie emailCookie = cookieService.getCookie("email", member.getEmail(), (int) 1000 * 60 * 30); // 30분 임시 쿠키
                response.addCookie(emailCookie);
            } else {
                // TODO: redis에 저장할 google access token 암호화!
                redisService.save(redisGoogleAtKey + ":" + member.getId().toString(), googleAccessToken, 1, TimeUnit.HOURS);

                CustomTokenRecord customAccessToken = tokenProvider.createAccessToken(member.getId(), member.getEmail());
                long ttl = customAccessToken.ttl();
                TimeUnit timeUnit = customAccessToken.timeUnit();

                Cookie accessTokenCookie = cookieService.getAccessTokenCookie(customAccessToken.token(), (int) timeUnit.toSeconds(ttl));
                response.addCookie(accessTokenCookie);
            }
            response.sendRedirect(frontendRedirectUrl + "/after-login");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
