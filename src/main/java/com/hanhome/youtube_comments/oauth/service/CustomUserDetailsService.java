package com.hanhome.youtube_comments.oauth.service;

import com.hanhome.youtube_comments.oauth.dto.CustomUserDetails;
import com.hanhome.youtube_comments.member.entity.Member;
import com.hanhome.youtube_comments.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String uuid) throws UsernameNotFoundException {
        UUID userId = UUID.fromString(uuid);
        Member member = memberRepository.findById(userId).orElse(null);

        if (member == null) {
            throw new UsernameNotFoundException("User not found with uuid: " + uuid);
        }
        return new CustomUserDetails(member);
    }
}
