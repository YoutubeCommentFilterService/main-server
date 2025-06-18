package com.hanhome.youtube_comments.google.controller;

import com.hanhome.youtube_comments.google.dto.PredictCategoryDto;
import com.hanhome.youtube_comments.google.service.PredictClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/metadata/predict-class")
public class PredictClassController {
    private final PredictClassService predictClassService;
    @GetMapping
    public ResponseEntity<PredictCategoryDto.Response> getPredictClasses() {
        return ResponseEntity.ok(predictClassService.getPredictClasses());
    }
}
