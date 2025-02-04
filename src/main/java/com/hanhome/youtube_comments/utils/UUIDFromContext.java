package com.hanhome.youtube_comments.utils;

import com.hanhome.youtube_comments.member.entity.Member;
import com.hanhome.youtube_comments.oauth.dto.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UUIDFromContext {
    public UUID getUUID() {
        Member member = getMember();
        return member.getId();
    }

    public Member getMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getMember();
    }
}
