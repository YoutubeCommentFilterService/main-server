package com.hanhome.youtube_comments.google.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PredictCategoryDto {
    List<String> nicknameCategories;
    List<String> commentCategories;
}
