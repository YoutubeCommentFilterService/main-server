package com.hanhome.youtube_comments.google.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class PredictCategoryDto {
    @Builder
    @Getter
    public static class Response {
        private List<String> nicknameCategories;
        private List<String> commentCategories;
    }
}
