package com.hanhome.youtube_comments.google.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.hanhome.youtube_comments.common.errors.CustomPredictServerException;
import com.hanhome.youtube_comments.common.response.PredictCommonResponse;
import com.hanhome.youtube_comments.google.dto.PredictCategoryDto;
import com.hanhome.youtube_comments.google.object.PredictServerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
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
    private final ObjectMapper objectMapper;

    public PredictCategoryDto.Response getPredictClasses() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme(predictServerProperties.getScheme())
                        .host(predictServerProperties.getHost())
                        .port(predictServerProperties.getPort())
                        .path("/predict-category")
                        .build()
                ).retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("Server Closed")
                                .flatMap(errorBody -> {
                                    JsonNode errorNode;
                                    try {
                                        errorNode = objectMapper.readTree(errorBody);
                                    } catch (Exception e) {
                                        errorNode = objectMapper.createObjectNode().put("error", errorBody);
                                    }
                                    return Mono.error(new CustomPredictServerException("Prediction Server Error", errorNode));
                                })
                )
                .bodyToMono(JsonNode.class)
                .flatMap(node -> {
                    PredictCommonResponse predictCommonResponse = PredictCommonResponse.builder().code(200).message("조회 성공").build();
                    ArrayNode nicknameCategoryNode = (ArrayNode) node.path("nickname_predict_class");
                    List<String> nicknameCategories = StreamSupport.stream(nicknameCategoryNode.spliterator(), false)
                            .map(JsonNode::asText)
                            .toList();

                    ArrayNode commentCategoryNode = (ArrayNode) node.path("comment_predict_class");
                    List<String> commentCategories = StreamSupport.stream(commentCategoryNode.spliterator(), false)
                            .map(JsonNode::asText).toList();

                    PredictCategoryDto.Response predictCategory = PredictCategoryDto.Response.builder()
                            .predictCommonResponse(predictCommonResponse)
                            .nicknameCategories(nicknameCategories)
                            .commentCategories(commentCategories)
                            .build();
                    return Mono.just(predictCategory);
                })
                .onErrorResume(CustomPredictServerException.class, ex -> {
                    PredictCommonResponse predictCommonResponse = PredictCommonResponse.builder().code(500).message(ex.getMessage()).build();
                    PredictCategoryDto.Response res = PredictCategoryDto.Response.builder()
                            .predictCommonResponse(predictCommonResponse)
                            .build();
                    return Mono.just(res);
                })
                .block();
    }
}
