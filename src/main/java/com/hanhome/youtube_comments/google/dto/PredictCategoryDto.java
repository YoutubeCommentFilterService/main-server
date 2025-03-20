package com.hanhome.youtube_comments.google.dto;

import com.hanhome.youtube_comments.common.response.PredictCommonResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class PredictCategoryDto {
    @Builder
    @Getter
    public static class Response {
        private PredictCommonResponse predictCommonResponse;
        private List<String> nicknameCategories;
        private List<String> commentCategories;
    }
}
