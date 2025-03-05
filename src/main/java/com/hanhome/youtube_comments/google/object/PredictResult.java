package com.hanhome.youtube_comments.google.object;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PredictResult {
    private PredictCategory predictCategory;
    private List<PredictResultItem> predictResultItems;
}
