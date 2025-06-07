package com.hanhome.youtube_comments.member.controller;

import com.hanhome.youtube_comments.member.dto.DeleteUserDto;
import com.hanhome.youtube_comments.member.dto.GetUserDto;
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
    public ResponseEntity<GetUserDto.Response> getUsers(@Validated @ModelAttribute GetUserDto.Request pagination) {
        return ResponseEntity.ok(adminService.getUsers(pagination));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteUSer(@Validated @ModelAttribute DeleteUserDto.Request request) throws Exception {
        adminService.deleteUser(request);
        return ResponseEntity.accepted().build();
    }
}
