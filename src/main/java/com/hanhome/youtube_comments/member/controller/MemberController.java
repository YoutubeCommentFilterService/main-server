package com.hanhome.youtube_comments.member.controller;

import com.hanhome.youtube_comments.member.dto.AccessTokenDto;
import com.hanhome.youtube_comments.member.dto.IsNewMemberDto;
import com.hanhome.youtube_comments.member.dto.RefreshTokenDto;
import com.hanhome.youtube_comments.member.entity.Member;
import com.hanhome.youtube_comments.member.service.MemberService;
import com.hanhome.youtube_comments.oauth.dto.CustomTokenRecord;
import com.hanhome.youtube_comments.oauth.dto.RenewAccessTokenDto;
import com.hanhome.youtube_comments.oauth.provider.JwtTokenProvider;
import com.hanhome.youtube_comments.oauth.service.CookieService;
import com.hanhome.youtube_comments.utils.UUIDFromContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Controller
@RequestMapping("/api/member")
public class MemberController {
    private final MemberService memberService;
    private final CookieService cookieService;
    private final UUIDFromContext uuidFromContext;
    private final JwtTokenProvider tokenProvider;

    @Value("${spring.app.default-domain}")
    private String defaultDomain;

    @GetMapping("/check-new")
    public ResponseEntity<IsNewMemberDto.Response> getIsNewMember(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            String email = getSpecificCookieVal(cookies, "email");
            String accessToken = getSpecificCookieVal(cookies, "access_token");

            if (!"".equals(email)) {
                boolean isNewMember = memberService.checkMemberIsNew(email);
                IsNewMemberDto.Response responseDto = IsNewMemberDto.Response.builder()
                        .isNewMember(isNewMember)
                        .build();
                return ResponseEntity.ok(responseDto);
            } else if (!"".equals(accessToken)) {
                IsNewMemberDto.Response responseDto = IsNewMemberDto.Response.builder()
                        .isNewMember(false)
                        .build();
                return ResponseEntity.ok(responseDto);
            }
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/accept-signin")
    public ResponseEntity<?> acceptSignin(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            String email = getAndRemoveSpecificCookie(response, cookies, "email");
            if ("".equals(email)) return ResponseEntity.badRequest().build();

            Member member = memberService.insert(email);
            if (member != null) {
                memberService.acceptSignup(email);
                CustomTokenRecord customAccessToken = tokenProvider.createAccessToken(member.getId(), member.getEmail());

                long ttl = customAccessToken.ttl();
                TimeUnit timeUnit = customAccessToken.timeUnit();

                Cookie accessTokenCookie = cookieService.getAccessTokenCookie(customAccessToken.token(), (int) timeUnit.toSeconds(ttl));
                response.addCookie(accessTokenCookie);

                return ResponseEntity.ok().build();
            }
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/reject-signin")
    public ResponseEntity<?> rejectSignin(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            String email = getAndRemoveSpecificCookie(response, cookies, "email");
            memberService.rejectSignup(email);
        }
        return ResponseEntity.noContent().build();
    }

    private String getSpecificCookieVal(Cookie[] cookies, String cookieKey) {
        for (Cookie cookie : cookies) {
            if (cookieKey.equals(cookie.getName())) return cookie.getValue();
        }
        return "";
    }

    private String getAndRemoveSpecificCookie(HttpServletResponse response, Cookie[] cookies, String cookieKey) {
        String cookieVal = getSpecificCookieVal(cookies, cookieKey);
        Cookie emailCookie = new Cookie(cookieKey, null);
        emailCookie.setDomain(defaultDomain);
        emailCookie.setMaxAge(0);
        emailCookie.setPath("/");
        response.addCookie(emailCookie);
        return cookieVal;
    }

    @GetMapping("/refresh-token")
    public ResponseEntity<RefreshTokenDto> getRefreshToken() {
        UUID uuid = uuidFromContext.getUUID();

        RefreshTokenDto refreshTokenDto = memberService.getRefreshToken(uuid);
        return ResponseEntity.ok(refreshTokenDto);
    }

    @PostMapping("/refresh-auth")
    public ResponseEntity<AccessTokenDto.Response> refreshAuth(HttpServletResponse response, @RequestBody RenewAccessTokenDto refreshDto) throws Exception {
        AccessTokenDto.Renew renewedAccessToken  = memberService.renewAccessToken(refreshDto);
        CustomTokenRecord customToken = renewedAccessToken.getCustomTokenRecord();
        String token = customToken.token();
        long ttl = customToken.ttl();
        TimeUnit timeUnit = customToken.timeUnit();

        Cookie tokenCookie = cookieService.getAccessTokenCookie(token, ((int) timeUnit.toSeconds(ttl)));
        response.addCookie(tokenCookie);

        AccessTokenDto.Response userProfile = AccessTokenDto.Response.builder()
                .profileImage(renewedAccessToken.getProfileImage())
                .nickname(renewedAccessToken.getNickname())
                .hasYoutubeAccess(renewedAccessToken.getHasYoutubeAccess())
                .role(renewedAccessToken.getRole())
                .build();
        return ResponseEntity.ok(userProfile);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        UUID uuid = uuidFromContext.getUUID();
        memberService.logout(uuid);

        Cookie cookie = cookieService.removeAccessTokenCookie();
        response.addCookie(cookie);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> withdraw(HttpServletResponse response) throws Exception  {
        UUID uuid = uuidFromContext.getUUID();
        memberService.withdraw(uuid);

        Cookie cookie = cookieService.removeAccessTokenCookie();
        response.addCookie(cookie);

        return ResponseEntity.noContent().build();
    }
}
