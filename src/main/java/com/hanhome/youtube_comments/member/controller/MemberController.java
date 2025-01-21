package com.hanhome.youtube_comments.member.controller;

import com.hanhome.youtube_comments.member.dto.RefreshTokenDto;
import com.hanhome.youtube_comments.member.service.MemberService;
import com.hanhome.youtube_comments.oauth.dto.CustomTokenRecord;
import com.hanhome.youtube_comments.oauth.dto.CustomUserDetails;
import com.hanhome.youtube_comments.oauth.dto.RenewAccessTokenDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Controller
@RequestMapping("/member")
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/refresh-token")
    public ResponseEntity<RefreshTokenDto> getRefreshToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID uuid = userDetails.getUuid();

        RefreshTokenDto refreshTokenDto = memberService.getRefreshToken(uuid);
        return ResponseEntity.ok(refreshTokenDto);
    }

    @PostMapping("/renew-token")
    public ResponseEntity<?> renewToken(HttpServletResponse response, @RequestBody RenewAccessTokenDto refreshDto) throws Exception {
        CustomTokenRecord customToken = memberService.renewAccessToken(refreshDto);
        String token = customToken.token();
        long ttl = customToken.ttl();
        TimeUnit timeUnit = customToken.timeUnit();

        Cookie tokenCookie = new Cookie("access_token", token);
        tokenCookie.setMaxAge((int) timeUnit.toSeconds(ttl));
        tokenCookie.setPath("/");
        tokenCookie.setHttpOnly(true);

        response.addCookie(tokenCookie);
        return null;
    }
}
