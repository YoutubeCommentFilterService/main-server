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
        if (cookies == null) ResponseEntity.badRequest().build();

        IsNewMemberDto.Response isNewMemberResponse = memberService.checkIsNewMemberFromCookie(cookies);
        if (isNewMemberResponse == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(isNewMemberResponse);
    }

    @PostMapping("/accept-signin")
    public ResponseEntity<?> acceptSignup(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return ResponseEntity.badRequest().build();

        boolean isSignupAccepted = memberService.acceptSignup(cookies, response);
        if (!isSignupAccepted) return ResponseEntity.badRequest().build();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reject-signin")
    public ResponseEntity<?> rejectSignin(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) memberService.rejectSignup(cookies, response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/refresh-token")
    public ResponseEntity<RefreshTokenDto> getRefreshToken() {
        UUID uuid = uuidFromContext.getUUID();

        RefreshTokenDto refreshTokenDto = memberService.getRefreshToken(uuid);
        return ResponseEntity.ok(refreshTokenDto);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AccessTokenDto.Response> refreshAuth(HttpServletResponse response, @RequestBody RenewAccessTokenDto refreshDto) {
//        AccessTokenDto.Renew renewedAccessToken = memberService.renewAccessToken(refreshDto, response);
//        CustomTokenRecord customToken = renewedAccessToken.getCustomTokenRecord();
//
//        AccessTokenDto.Response userProfile = AccessTokenDto.Response.builder()
//                .profileImage(renewedAccessToken.getProfileImage())
//                .nickname(renewedAccessToken.getNickname())
//                .hasYoutubeAccess(renewedAccessToken.getHasYoutubeAccess())
//                .role(renewedAccessToken.getRole())
//                .build();
//        String token = customToken.token();
//        long ttl = customToken.ttl();
//        TimeUnit timeUnit = customToken.timeUnit();
//
//        Cookie tokenCookie = cookieService.getAccessTokenCookie(token, ((int) timeUnit.toSeconds(ttl)));
//        response.addCookie(tokenCookie);
        return ResponseEntity.ok(memberService.renewAccessToken(refreshDto, response));
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
