package com.hanhome.youtube_comments.oauth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

//@Component
public class CsrfDebugFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String headerToken = request.getHeader("X-XSRF-TOKEN");
        String cookieToken = Arrays.stream(request.getCookies())
                .filter(cookie -> "XSRF-TOKEN".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse("");
        HttpSession session = request.getSession(false);
        String sessionCsrfToken = null;
        if (session != null) {
            Object sessionCsrf = session.getAttribute("_csrf");
            if (sessionCsrf instanceof CsrfToken) sessionCsrfToken = ((CsrfToken) sessionCsrf).getToken();

            System.out.println("Session ID: " + session.getId());
        }

        System.out.println("X-XSRF-TOKEN (Header): " + headerToken);
        System.out.println("XSRF-TOKEN (Cookie): " + cookieToken);
        System.out.println("CSRF-TOKEN (Session): " + sessionCsrfToken);
        filterChain.doFilter(request, response);
    }
}
