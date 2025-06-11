package com.hanhome.youtube_comments.member.service;

import com.hanhome.youtube_comments.member.dto.*;
import com.hanhome.youtube_comments.member.entity.Member;
import com.hanhome.youtube_comments.member.object.MemberRole;
import com.hanhome.youtube_comments.member.object.SimpleMemberInfo;
import com.hanhome.youtube_comments.member.repository.MemberRepository;
import com.hanhome.youtube_comments.oauth.dto.CustomUserDetails;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
@Service
public class AdminService {
    private final MemberRepository memberRepository;
    private final MemberService memberService;

    @PreAuthorize("hasRole('ADMIN')")
    public GetMemberDto.Response getUsers(GetMemberDto.Request pagination) {
        int page = pagination.getPage() - 1;
        int take = pagination.getTake();
        String searchPattern = pagination.getChannelName();

        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Sort.Direction direction = Sort.Direction.fromString("DESC");
        PageRequest pageable = PageRequest.of(page, take, Sort.by(direction, "createdAt"));

        UUID adminUUID = userDetails.getMember().getId();

        Page<Member> result = (searchPattern == null || searchPattern.trim().isEmpty())
            ? memberRepository.findByIdNot(adminUUID, pageable)
            : memberRepository.findByChannelNameContainingAndIdNot(searchPattern.trim(), adminUUID, pageable);
        List<SimpleMemberInfo> members = result.get().map(Member::toSimpleInfo).toList();

        return GetMemberDto.Response.builder()
                .totalMembers(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .members(members)
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteUser(DeleteUserDto.Request request) {
        String targetChannelId = request.getChannelId();
        UUID targetUserId = UUID.fromString(request.getUserId());
        Optional<Member> targetMember = memberRepository.findByChannelId(targetChannelId)
                .or(() -> memberRepository.findById(targetUserId));
        if (targetMember.isPresent()) {
            UUID targetUserUUID = targetMember.get().getId();
            memberService.withdraw(targetUserUUID);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public boolean updateUserRole(UpdateUserRoleDto.Request updateDto) {
        System.out.println(updateDto);
        UUID targetUserId = UUID.fromString(updateDto.getUserId());
        String role = updateDto.getRole();

        MemberRole toUpdateRole = MemberRole.valueOf(role);

        Optional<Member> memberOpt = memberRepository.findById(targetUserId);
        memberOpt.ifPresent(member -> member.setRole(toUpdateRole));

        return memberOpt.isPresent();
    }

    @Cacheable(value = "availableRoles")
    public AvailableRolesDto.Response getAvailableRoles() {
        List<String> availableRoles = Arrays.stream(MemberRole.values())
                .filter(role -> role != MemberRole.UNLINKED)
                .map(Enum::name)
                .toList();
        System.out.println(availableRoles);
        return AvailableRolesDto.Response.builder()
                .availableRoles(availableRoles)
                .build();
    }
}
