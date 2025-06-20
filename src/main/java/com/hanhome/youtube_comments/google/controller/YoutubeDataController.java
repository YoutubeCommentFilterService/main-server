package com.hanhome.youtube_comments.google.controller;

import com.hanhome.youtube_comments.google.dto.DeleteCommentsDto;
import com.hanhome.youtube_comments.google.dto.GetCommentsDto;
import com.hanhome.youtube_comments.google.dto.GetHotVideosDto;
import com.hanhome.youtube_comments.google.dto.GetVideosDto;
import com.hanhome.youtube_comments.google.service.YoutubeDataService;
import com.hanhome.youtube_comments.member.entity.Member;
import com.hanhome.youtube_comments.utils.UUIDFromContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
public class YoutubeDataController {
    private final YoutubeDataService youtubeDataService;
    private final UUIDFromContext uuidFromContext;

    @GetMapping("/videos")
    public ResponseEntity<GetVideosDto.Response> getVideos(
            @Valid @ModelAttribute GetVideosDto.Request requestDto
    ) throws Exception {
        Member member = uuidFromContext.getMember();
        return ResponseEntity.ok(youtubeDataService.getVideosByPlaylist(requestDto, member));
    }

    @GetMapping("/hot-videos")
    public ResponseEntity<GetHotVideosDto.Response> getHotVideos() throws Exception {
        GetHotVideosDto.Response response = youtubeDataService.getHotVideos();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/videos/{videoId}")
    public ResponseEntity<GetCommentsDto.Response> getCommentsByVideoId(
            @PathVariable @Size(min = 1) String videoId
    ) throws Exception {
        Member member = uuidFromContext.getMember();
        return ResponseEntity.ok(youtubeDataService.getCommentsByVideoId(videoId, member));
    }

    @DeleteMapping("/comments")
    public ResponseEntity<Void> deleteComments(@RequestBody DeleteCommentsDto.Request requestDto) throws Exception {
        Member member = uuidFromContext.getMember();
        youtubeDataService.updateModerationAndAuthorBan(requestDto, member);
        return ResponseEntity.noContent().build();
    }
}
