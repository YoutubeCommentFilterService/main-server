package com.hanhome.youtube_comments.oauth.service;

import jakarta.servlet.http.Cookie;
import org.springframework.stereotype.Service;

@Service
public class CookieService {
    public Cookie getCookie(String cookieName, String cookieValue, int maxAge) {
        Cookie cookie = new Cookie(cookieName, cookieValue);
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        return cookie;
    }

    public Cookie getAccessTokenCookie(String cookieValue, int maxAge) {
        return getCookie("access_token", cookieValue, maxAge);
    }

    public Cookie removeAccessTokenCookie() {
        return getCookie("access_token", "", 0);
    }
}
