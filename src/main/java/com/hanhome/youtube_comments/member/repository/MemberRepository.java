package com.hanhome.youtube_comments.member.repository;

import com.hanhome.youtube_comments.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByChannelId(String channelId);
    Page<Member> findByIdNot(UUID id, Pageable pageable);
    void deleteByEmail(String email);
    Page<Member> findByChannelNameContainingAndIdNot(String channelName, UUID id, Pageable pageable);
}
