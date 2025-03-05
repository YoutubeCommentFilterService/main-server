package com.hanhome.youtube_comments.google.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.hanhome.youtube_comments.google.dto.PredictCategoryDto;
import com.hanhome.youtube_comments.google.object.PredictServerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class PredictClassService {
    private final PredictServerProperties predictServerProperties;
    private final WebClient webClient = WebClient.create();

    public PredictCategoryDto getPredictClasses() {
        PredictCategoryDto categories = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme(predictServerProperties.getScheme())
                        .host(predictServerProperties.getHost())
                        .port(predictServerProperties.getPort())
                        .path("/predict-category")
                        .build()
                ).retrieve().bodyToMono(JsonNode.class).flatMap(node -> {
                    ArrayNode nicknameCategoryNode = (ArrayNode) node.path("nickname_predict_class");
                    List<String> nicknameCategories = StreamSupport.stream(nicknameCategoryNode.spliterator(), false)
                            .map(JsonNode::asText)
                            .toList();

                    ArrayNode commentCategoryNode = (ArrayNode) node.path("comment_predict_class");
                    List<String> commentCategories = StreamSupport.stream(commentCategoryNode.spliterator(), false)
                            .map(JsonNode::asText).toList();

                    PredictCategoryDto predictCategory = PredictCategoryDto.builder()
                            .nicknameCategories(nicknameCategories)
                            .commentCategories(commentCategories)
                            .build();
                    return Mono.just(predictCategory);
                })
                .block();
        return categories;
    }
}
