package com.hanhome.youtube_comments.member.controller;

import com.hanhome.youtube_comments.member.dto.RefreshTokenDto;
import com.hanhome.youtube_comments.member.service.MemberService;
import com.hanhome.youtube_comments.oauth.dto.CustomTokenRecord;
import com.hanhome.youtube_comments.oauth.dto.CustomUserDetails;
import com.hanhome.youtube_comments.oauth.dto.RenewAccessTokenDto;
import com.hanhome.youtube_comments.oauth.service.CookieService;
import com.hanhome.youtube_comments.utils.UUIDFromContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @GetMapping("/refresh-token")
    public ResponseEntity<RefreshTokenDto> getRefreshToken() {
        UUID uuid = uuidFromContext.getUUID();

        RefreshTokenDto refreshTokenDto = memberService.getRefreshToken(uuid);
        return ResponseEntity.ok(refreshTokenDto);
    }

    @PostMapping("/renew-token")
    public ResponseEntity<Void> renewToken(HttpServletResponse response, @RequestBody RenewAccessTokenDto refreshDto) throws Exception {
        CustomTokenRecord customToken = memberService.renewAccessToken(refreshDto);
        String token = customToken.token();
        long ttl = customToken.ttl();
        TimeUnit timeUnit = customToken.timeUnit();

        Cookie tokenCookie = cookieService.getAccessTokenCookie(token, ((int) timeUnit.toSeconds(ttl)));
        response.addCookie(tokenCookie);
        return ResponseEntity.noContent().build();
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
    public ResponseEntity<Void> withdraw(HttpServletResponse response) {
        UUID uuid = uuidFromContext.getUUID();
        memberService.withdraw(uuid);

        Cookie cookie = cookieService.removeAccessTokenCookie();
        response.addCookie(cookie);

        return ResponseEntity.noContent().build();
    }
}
