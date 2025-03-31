package com.hanhome.youtube_comments.google.controller;

import com.hanhome.youtube_comments.google.dto.DeleteCommentsDto;
import com.hanhome.youtube_comments.google.dto.GetCommentsDto;
import com.hanhome.youtube_comments.google.dto.GetVideosDto;
import com.hanhome.youtube_comments.google.service.YoutubeDataService;
import com.hanhome.youtube_comments.member.entity.Member;
import com.hanhome.youtube_comments.utils.UUIDFromContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
public class YoutubeDataController {
    private final YoutubeDataService commentService;
    private final UUIDFromContext uuidFromContext;

    @GetMapping("/videos")
    public ResponseEntity<GetVideosDto.Response> getVideos(
            @Valid @ModelAttribute GetVideosDto.Request requestDto
    ) throws Exception {
        Member member = uuidFromContext.getMember();
        return ResponseEntity.ok(commentService.getVideosByPlaylist(requestDto, member));
    }

    @GetMapping("/comments/by-channel")
    public ResponseEntity<GetCommentsDto.Response> getCommentByChannelId(
            @Valid @ModelAttribute GetCommentsDto.Request requestDto
    ) throws Exception {
        Member member = uuidFromContext.getMember();
        return ResponseEntity.ok(commentService.getCommentsByChannelId(requestDto, member));
    }

    @GetMapping("/videos/{videoId}")
    public ResponseEntity<GetCommentsDto.Response> getCommentsByVideoId(
            @Valid @ModelAttribute GetCommentsDto.Request requestDto,
            @PathVariable @Size(min = 1) String videoId
    ) throws Exception {
        Member member = uuidFromContext.getMember();
        return ResponseEntity.ok(commentService.getCommentsByVideoId(requestDto, videoId, member));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteComments(@RequestBody DeleteCommentsDto.Request requestDto) throws Exception {
        Member member = uuidFromContext.getMember();
        commentService.updateModerationAndAuthorBan(requestDto, member);
        return ResponseEntity.noContent().build();
    }
}
