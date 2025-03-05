package com.hanhome.youtube_comments.google.object;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PredictResult {
    private List<PredictResultItem> predictResultItems;
}
