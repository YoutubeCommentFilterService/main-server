package com.hanhome.youtube_comments.oauth.service;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CookieService {
    @Value("${spring.app.default-domain}")
    private String defaultDomain;

    public Cookie getCookie(String cookieName, String cookieValue, int maxAge) {
        Cookie cookie = new Cookie(cookieName, cookieValue);
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "None");
        cookie.setSecure(true);
        cookie.setDomain(defaultDomain);
        return cookie;
    }

    public Cookie getAccessTokenCookie(String cookieValue, int maxAge) {
        return getCookie("access_token", cookieValue, maxAge);
    }

    public Cookie removeAccessTokenCookie() {
        return getCookie("access_token", "", 0);
    }
}
