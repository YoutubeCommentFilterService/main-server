package com.hanhome.youtube_comments.google.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.hanhome.youtube_comments.google.dto.DeleteCommentsDto;
import com.hanhome.youtube_comments.google.dto.GetCommentsDto;
import com.hanhome.youtube_comments.google.dto.GetVideosDto;
import com.hanhome.youtube_comments.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class YoutubeDataService {
    private final RedisService redisService;
    private final RenewGoogleTokenService googleTokenService;

    private final WebClient webClient = WebClient.create();
    private final static int VIDEO_MAX_RESULT = 50;
    private final static int COMMENT_MAX_RESULT = 100;
    private final static String VIDEO_REDIS_KEY = "NEXT_VIDEO";
    private final static String COMMENT_REDIS_KEY = "NEXT_COMMENT";
    private final static int STEP = 10;

    @Value("${data.youtube.api-key}")
    private String apiKey;

    @Value("${data.youtube.access-token}")
    private String redisGoogleAtKey;

    private String getGoogleAccessToken(UUID uuid) {
        String googleAccessToken = (String) redisService.get(redisGoogleAtKey + ":" + uuid);
        return googleAccessToken == null ? googleTokenService.renewAccessToken(uuid) : googleAccessToken;
    }

    public GetVideosDto.FromGoogle getVideos(GetVideosDto.Request request, UUID uuid) {
        String googleAccessToken = getGoogleAccessToken(uuid);

        Map<String, Object> queries = generateDefaultQueries();
        queries.put("part", "snippet");
        queries.put("order", "date");
        queries.put("forMine", true);
        queries.put("type", "video");

        int maxResult = request.getTake() == null ? VIDEO_MAX_RESULT : request.getTake();
        queries.put("maxResults", maxResult);

        if (request.getPage() != 1) {
            String nextToken = (String) redisService.get(VIDEO_REDIS_KEY + ":" + uuid);
            if (nextToken != null) queries.put("pageToken", nextToken);
        }

        GetVideosDto.FromGoogle fromGoogle = webClient.get()
                .uri(uriBuilder -> generateUri(uriBuilder, queries, "search"))
                .headers(headers -> setCommonHeader(headers, googleAccessToken))
                .retrieve()
                .onStatus( // 401 - 잘못된 토큰, 403 - unauthorized
                        HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new BadRequestException("Re-login google"))
                )
                .bodyToMono(JsonNode.class)
                .flatMap(this::generateVideosResponse)
                .block();

        if (fromGoogle.getTotalResults() == maxResult && !"".equals(fromGoogle.getNextPageToken())) {
            redisService.save(VIDEO_REDIS_KEY + ":" + uuid, fromGoogle.getNextPageToken(), 30, TimeUnit.MINUTES);
            fromGoogle.setIsLast(false);
        } else {
            redisService.remove(VIDEO_REDIS_KEY + ":" + uuid);
            fromGoogle.setIsLast(true);
        }

        return fromGoogle;
    }

    public GetCommentsDto.Response getComments(GetCommentsDto.Request request, String videoId, UUID uuid) {
        int step = 0;
        boolean isLast = false;
        String googleAccessToken = getGoogleAccessToken(uuid);

        Map<String, Object> queries = generateDefaultQueries();
        queries.put("part", "snippet");
        queries.put("videoId", videoId);

        int maxResult = request.getTake() == null ? COMMENT_MAX_RESULT : request.getTake();
        queries.put("maxResults", maxResult);

        if (request.getPage() != 1) {
            String nextToken = (String) redisService.get(COMMENT_REDIS_KEY + ":" + uuid);
            if (nextToken != null) queries.put("pageToken", nextToken);
        }

        GetCommentsDto.FromGoogle fromGoogle = null;
        List<GetCommentsDto.FromGoogle.CommentThreadResource> resources = new ArrayList<>();
        for (step = 0; step < STEP; step++) {
            fromGoogle = webClient.get()
                    .uri(uriBuilder -> generateUri(uriBuilder, queries, "commentThreads"))
                    .headers(headers -> setCommonHeader(headers, googleAccessToken))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .flatMap(this::generateCommentsResponse)
                    .block();

            resources.addAll(fromGoogle.getItems());

            if (fromGoogle.getTotalResults() == maxResult && !"".equals(fromGoogle.getNextPageToken())) {
                queries.put("pageToken", fromGoogle.getNextPageToken());
            } else {
                redisService.remove(COMMENT_REDIS_KEY + ":" + uuid);
                isLast = true;
                break;
            }
        }

        if (fromGoogle.getTotalResults() == maxResult && !"".equals(fromGoogle.getNextPageToken())) {
            redisService.save(COMMENT_REDIS_KEY + ":" + uuid, fromGoogle.getNextPageToken(), 30, TimeUnit.MINUTES);
            isLast = false;
        }

        return GetCommentsDto.Response.builder()
                .items(resources)
                .isLast(isLast)
                .build();
    }

    public void updateModerationAndAuthorBan(DeleteCommentsDto.Request request, UUID uuid) {
        String googleAccessToken = getGoogleAccessToken(uuid);

        // Remove Comment + Author Ban
        if (request.getAuthorBanComments() != null && !request.getAuthorBanComments().isEmpty()) {
            Map<String, Object> authorBanQueries = generateDefaultQueries();
            authorBanQueries.put("id", request.getAuthorBanComments());
            authorBanQueries.put("moderationStatus", "rejected");
            authorBanQueries.put("banAuthor", true);

            webClient.post()
                    .uri(uriBuilder -> generateUri(uriBuilder, authorBanQueries, "setModerationStatus"))
                    .headers(headers -> setCommonHeader(headers, googleAccessToken))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        }

        // Just Remove Comment
        if (request.getJustDeleteComments() != null && !request.getJustDeleteComments().isEmpty()) {
            Map<String, Object> justRemoveQueries = generateDefaultQueries();
            justRemoveQueries.put("id", request.getJustDeleteComments());
            justRemoveQueries.put("moderationStatus", "rejected");

            webClient.post()
                    .uri(uriBuilder -> generateUri(uriBuilder, justRemoveQueries, "setModerationStatus"))
                    .headers(headers -> setCommonHeader(headers, googleAccessToken))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        }
    }

    private Map<String, Object> generateDefaultQueries() {
        Map<String, Object> queries = new HashMap<>();

        queries.put("key", apiKey);
        return queries;
    }

    private Mono<GetVideosDto.FromGoogle> generateVideosResponse(JsonNode rootNode) {
        JsonNode pageInfo = rootNode.path("pageInfo");

        List<GetVideosDto.FromGoogle.VideoResource> resultVideoResources = new ArrayList<>();
        ArrayNode items = (ArrayNode) rootNode.path("items");
        items.forEach(item -> {
            JsonNode snippet = item.path("snippet");
            String publishedAt = snippet.path("publishedAt").asText();
            ZonedDateTime zoneDateTime = ZonedDateTime.parse(publishedAt);

            GetVideosDto.FromGoogle.VideoResource resource =
                    GetVideosDto.FromGoogle.VideoResource.builder()
                        .videoId(item.path("id").path("videoId").asText(""))
                        .videoTitle(snippet.path("title").asText(""))
                        .videoThumb(snippet.path("thumbnails").path("medium").path("url").asText(""))
                        .publishedAt(zoneDateTime.toLocalDate())
                        .build();

            resultVideoResources.add(resource);
        });

        GetVideosDto.FromGoogle fromGoogle = GetVideosDto.FromGoogle.builder()
                .videoResources(resultVideoResources)
                .totalResults(pageInfo.path("totalResults").asInt(0))
                .nextPageToken(rootNode.path("nextPageToken").asText(""))
                .build();

        return Mono.just(fromGoogle);
    }

    private Mono<GetCommentsDto.FromGoogle> generateCommentsResponse(JsonNode rootPath) {
        JsonNode pageInfo = rootPath.path("pageInfo");

        List<GetCommentsDto.FromGoogle.CommentThreadResource> resultCommentResources = new ArrayList<>();
        ArrayNode items = (ArrayNode) rootPath.path("items");
        items.forEach(item -> {
            String commentId = item.path("id").asText("");

            JsonNode snippet = item.path("snippet").path("topLevelComment").path("snippet");
            String authorDisplayName = snippet.path("authorDisplayName").asText("");
            String textOriginal = snippet.path("textOriginal").asText("");

            GetCommentsDto.FromGoogle.CommentThreadResource resource =
                    GetCommentsDto.FromGoogle.CommentThreadResource.builder()
                        .commentId(commentId)
                        .textOriginal(textOriginal)
                        .authorNickname(authorDisplayName)
                        .build();

            resultCommentResources.add(resource);
        });

        GetCommentsDto.FromGoogle fromGoogle = GetCommentsDto.FromGoogle.builder()
                .items(resultCommentResources)
                .totalResults(pageInfo.path("totalResults").asInt(0))
                .nextPageToken(rootPath.path("nextPageToken").asText(""))
                .build();

        return Mono.just(fromGoogle);
    }

    private URI generateUri(UriBuilder uriBuilder, Map<String, Object> queries, String endpoint) {
        uriBuilder.scheme("https")
                .host("www.googleapis.com")
                .path("/youtube/v3/" + endpoint);
        queries.forEach(uriBuilder::queryParam);
        return uriBuilder.build();
    }

    private void setCommonHeader(HttpHeaders headers, String googleAccessToken) {
        headers.add("Authorization", "Bearer " + googleAccessToken);
        headers.add("Accept", "application/json");
    }
}
