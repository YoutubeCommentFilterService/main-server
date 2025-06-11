package com.hanhome.youtube_comments.member.controller;

import com.hanhome.youtube_comments.member.dto.AvailableRolesDto;
import com.hanhome.youtube_comments.member.dto.DeleteUserDto;
import com.hanhome.youtube_comments.member.dto.GetMemberDto;
import com.hanhome.youtube_comments.member.dto.UpdateUserRoleDto;
import com.hanhome.youtube_comments.member.service.AdminService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<GetMemberDto.Response> getUsers(@Validated @ModelAttribute GetMemberDto.Request pagination) {
        return ResponseEntity.ok(adminService.getUsers(pagination));
    }

    @GetMapping("/roles")
    public ResponseEntity<AvailableRolesDto.Response> getAvailableRoles() {
        return ResponseEntity.ok(adminService.getAvailableRoles());
    }

    @PatchMapping("/member/role")
    public ResponseEntity<?> updateUserRole(@RequestBody UpdateUserRoleDto.Request updateDto) {
        boolean result = adminService.updateUserRole(updateDto);
        return result ? ResponseEntity.ok().build() : ResponseEntity.noContent().build();
    }

    @DeleteMapping("/member")
    public ResponseEntity<?> deleteUSer(@ModelAttribute DeleteUserDto.Request requestDto) throws Exception {
        adminService.deleteUser(requestDto);
        return ResponseEntity.noContent().build();
    }
}
