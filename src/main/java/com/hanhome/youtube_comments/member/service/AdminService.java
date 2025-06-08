package com.hanhome.youtube_comments.member.service;

import com.hanhome.youtube_comments.member.dto.DeleteUserDto;
import com.hanhome.youtube_comments.member.dto.GetUserDto;
import com.hanhome.youtube_comments.member.entity.Member;
import com.hanhome.youtube_comments.member.object.SimpleMemberInfo;
import com.hanhome.youtube_comments.member.repository.MemberRepository;
import com.hanhome.youtube_comments.oauth.dto.CustomUserDetails;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
@Service
public class AdminService {
    private final MemberRepository memberRepository;
    private final MemberService memberService;

    @PreAuthorize("hasRole('ADMIN')")
    public GetUserDto.Response getUsers(GetUserDto.Request pagination) {
        int page = pagination.getPage() - 1;
        int take = pagination.getTake();

        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Sort.Direction direction = Sort.Direction.fromString("DESC");
        PageRequest pageable = PageRequest.of(page, take, Sort.by(direction, "createdAt"));

        Page<Member> result = memberRepository.findByIdNot(userDetails.getMember().getId(), pageable);
        List<SimpleMemberInfo> members = result.get().map(Member::toSimpleInfo).toList();

        return GetUserDto.Response.builder()
                .totalMembers(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .members(members)
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteUser(DeleteUserDto.Request request) throws Exception {
        String targetChannelId = request.getChannelId();
        UUID targetUserId = UUID.fromString(request.getUserId());
        Optional<Member> targetMember = memberRepository.findByChannelId(targetChannelId)
                .or(() -> memberRepository.findById(targetUserId));
        if (targetMember.isPresent()) {
            UUID targetUserUUID = targetMember.get().getId();
            memberService.withdraw(targetUserUUID);
        }
    }
}
