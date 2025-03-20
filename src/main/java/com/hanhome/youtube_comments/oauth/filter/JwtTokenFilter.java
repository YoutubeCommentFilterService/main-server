package com.hanhome.youtube_comments.oauth.filter;

import com.hanhome.youtube_comments.oauth.dto.CustomUserDetails;
import com.hanhome.youtube_comments.oauth.provider.JwtTokenProvider;
import com.hanhome.youtube_comments.oauth.service.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@RequiredArgsConstructor
@Component
@Slf4j
public class JwtTokenFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (shouldSkipFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Cookie tokenCookie = null;
        if (request.getCookies() != null) {
            tokenCookie = getTokenCookie(request.getCookies());
        }

        if (tokenCookie == null) {
            sendUnauthorizedError(response, "No JWT Access Token Found");
            return;
        }

        String accessToken = tokenCookie.getValue();
        try {
            Claims claims = tokenProvider.validate(accessToken);

            String uuid = claims.getSubject();
            Authentication authentication = getAuthentication(uuid);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (UsernameNotFoundException e) {
            sendUnauthorizedError(response, "User not found: " + e.getMessage());
        } catch (RuntimeException e) {
            sendUnauthorizedError(response, "Invalid token: " + e.getMessage());
        }
    }

    private boolean shouldSkipFilter(HttpServletRequest request) {
        String[] publicUrls = {"/api/member/renew-token", "/api/metadata/class",
                "/api/member/is-new-member", "/api/member/confirm-signin", "/api/member/reject-signin"
        };
        String path = request.getRequestURI();
        return Arrays.stream(publicUrls).anyMatch(path::startsWith);
    }

    private void sendUnauthorizedError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"%s\"}", message));
    }

    private Cookie getTokenCookie(Cookie[] cookies) {
        return Arrays.stream(cookies)
                .filter(cookie -> "access_token".equals(cookie.getName()))
                .findFirst()
                .orElse(null);
    }

    private Authentication getAuthentication(String uuid) {
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(uuid);
        return new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
    }
}
