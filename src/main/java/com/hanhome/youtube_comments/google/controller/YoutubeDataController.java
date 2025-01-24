package com.hanhome.youtube_comments.google.controller;

import com.hanhome.youtube_comments.google.dto.DeleteCommentsDto;
import com.hanhome.youtube_comments.google.dto.GetCommentsDto;
import com.hanhome.youtube_comments.google.dto.GetVideosDto;
import com.hanhome.youtube_comments.google.service.YoutubeDataService;
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

    @GetMapping
    public ResponseEntity<GetVideosDto.FromGoogle> getVideos(
            @Valid @ModelAttribute GetVideosDto.Request requestDto
    ) {
        UUID uuid = uuidFromContext.getUUID();
        return ResponseEntity.ok(commentService.getVideos(requestDto, uuid));
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<GetCommentsDto.Response> getComments(
            @Valid @ModelAttribute GetCommentsDto.Request requestDto,
            @PathVariable @Size(min = 1) String videoId
    ) {
        UUID uuid = uuidFromContext.getUUID();
        return ResponseEntity.ok(commentService.getComments(requestDto, videoId, uuid));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteComments(@ModelAttribute DeleteCommentsDto.Request requestDto) {
        UUID uuid = uuidFromContext.getUUID();
        commentService.updateModerationAndAuthorBan(requestDto, uuid);
        return ResponseEntity.noContent().build();
    }
}
