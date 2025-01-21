package com.hanhome.youtube_comments.google.controller;

import com.hanhome.youtube_comments.google.dto.GetCommentsDto;
import com.hanhome.youtube_comments.google.dto.GetVideosDto;
import com.hanhome.youtube_comments.google.service.RenewGoogleTokenService;
import com.hanhome.youtube_comments.google.service.YoutubeDataService;
import com.hanhome.youtube_comments.oauth.dto.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
public class YoutubeDataController {
    private final YoutubeDataService commentService;
    private final RenewGoogleTokenService googleTokenService;

    @GetMapping
    public ResponseEntity<GetVideosDto.FromGoogle> getVideos(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute GetVideosDto.Request request
    ) {
        UUID uuid = userDetails.getUuid();
        return ResponseEntity.ok(commentService.getVideos(request, uuid));
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<GetCommentsDto.Response> getComments(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute GetCommentsDto.Request request,
            @PathVariable @Size(min = 1) String videoId
    ) {
        UUID uuid = userDetails.getUuid();
        return ResponseEntity.ok(commentService.getComments(request, videoId, uuid));
    }
}
