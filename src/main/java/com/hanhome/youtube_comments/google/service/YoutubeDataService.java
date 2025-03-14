package com.hanhome.youtube_comments.google.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.hanhome.youtube_comments.google.dto.CommentPredictDto;
import com.hanhome.youtube_comments.google.dto.DeleteCommentsDto;
import com.hanhome.youtube_comments.google.dto.GetCommentsDto;
import com.hanhome.youtube_comments.google.dto.GetVideosDto;
import com.hanhome.youtube_comments.google.object.*;
import com.hanhome.youtube_comments.member.entity.Member;
import com.hanhome.youtube_comments.member.object.YoutubeAccountDetail;
import com.hanhome.youtube_comments.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class YoutubeDataService {
    private final RedisService redisService;
    private final RenewGoogleTokenService googleTokenService;
    private final PredictServerProperties predictServerProperties;

    private final WebClient webClient = WebClient.create();
    private final static int VIDEO_MAX_RESULT = 50;
    private final static int COMMENT_MAX_RESULT = 100;
    private final static String VIDEO_REDIS_KEY = "NEXT_VIDEO";
    private final static String VIDEO_COMMENT_REDIS_KEY = "NEXT_COMMENT_V";
    private final static String CHANNEL_COMMENT_REDIS_KEY = "NEXT_COMMENT_C";
    private final static int STEP = 10;

    @Value("${data.youtube.api-key}")
    private String apiKey;

    @Value("${data.youtube.access-token}")
    private String redisGoogleAtKey;

    private String getGoogleAccessToken(UUID uuid) throws Exception {
        String googleAccessToken = (String) redisService.get(redisGoogleAtKey + ":" + uuid);
        return googleAccessToken == null ? googleTokenService.renewAccessToken(uuid) : googleAccessToken;
    }

    public String getChannelId(String googleAccessToken) {
        Map<String, Object> queries = generateDefaultQueries();
        queries.put("part", "id,contentDetails");
        queries.put("mine", true);

        return webClient.get()
                .uri(uriBuilder -> generateUri(uriBuilder, queries, "channels"))
                .headers(headers -> setCommonHeader(headers, googleAccessToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(this::generateChannelId)
                .block();
    }

    public YoutubeAccountDetail getYoutubeAccountDetail(String googleAccessToken) {
        Map<String, Object> queries = generateDefaultQueries();
        queries.put("part", "id,contentDetails");
        queries.put("mine", true);

        return webClient.get()
                .uri(uriBuilder -> generateUri(uriBuilder, queries, "channels"))
                .headers(headers -> setCommonHeader(headers, googleAccessToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(this::generateYoutubeChannelDetail)
                .block();
    }

    public GetVideosDto.Response getVideos(GetVideosDto.Request request, Member member) throws Exception {
        UUID uuid = member.getId();
        String googleAccessToken = getGoogleAccessToken(uuid);

        Map<String, Object> queries = generateDefaultQueries();
        queries.put("part", "snippet");
        queries.put("order", "date");
        queries.put("forMine", true);
        queries.put("type", "video");

        int maxResult = request.getTake() == null ? VIDEO_MAX_RESULT : request.getTake();
        queries.put("maxResults", maxResult);

        if (request.getPage() != 1) {
            putNextPageTokenQuery(queries, VIDEO_REDIS_KEY, uuid);
        }

        GetVideosDto.FromGoogle fromGoogle = webClient.get()
                .uri(uriBuilder -> generateUri(uriBuilder, queries, "search"))
                .headers(headers -> setCommonHeader(headers, googleAccessToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap((rootNode) -> generateVideosResponse(rootNode, true))
                .block();

        if (!"".equals(fromGoogle.getNextPageToken())) {
            redisService.save(VIDEO_REDIS_KEY + ":" + uuid, fromGoogle.getNextPageToken(), 30, TimeUnit.MINUTES);
            fromGoogle.setIsLast(false);
        } else {
            redisService.remove(VIDEO_REDIS_KEY + ":" + uuid);
            fromGoogle.setIsLast(true);
        }

        return GetVideosDto.Response.builder()
                .isLast(fromGoogle.getIsLast() ? "Y" : "N")
                .items(fromGoogle.getVideoResources())
                .build();
    }

    public GetVideosDto.Response getVideosByPlaylist(GetVideosDto.Request request, Member member) throws Exception {
        UUID uuid = member.getId();
        String playlistId = member.getPlaylistId();
        String googleAccessToken = getGoogleAccessToken(uuid);

        if (playlistId.isEmpty()) return GetVideosDto.Response.builder()
                .isLast("Y")
                .items(new ArrayList<>())
                .build();

        Map<String, Object> getVideoQuery = generateDefaultQueries();

        getVideoQuery.put("part", "snippet");
        getVideoQuery.put("playlistId", playlistId);

        int maxResult = request.getTake() == null ? VIDEO_MAX_RESULT : request.getTake();
        getVideoQuery.put("maxResults", maxResult);

        if (request.getPage() != 1) {
            putNextPageTokenQuery(getVideoQuery, VIDEO_REDIS_KEY, uuid);
        }

        GetVideosDto.FromGoogle fromGoogle = webClient.get()
                .uri(uriBuilder -> generateUri(uriBuilder, getVideoQuery, "playlistItems"))
                .headers(headers -> setCommonHeader(headers, googleAccessToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap((rootNode) -> generateVideosResponse(rootNode, false))
                .block();

        Map<String, Object> getVideoInfoQuery = generateDefaultQueries();
        String videoIdsQuery = fromGoogle.getVideoResources().stream().map(YoutubeVideo::getId).collect(Collectors.joining(","));
        getVideoInfoQuery.put("part", "snippet,status");
        getVideoInfoQuery.put("id", videoIdsQuery);
        getVideoInfoQuery.put("maxResults", maxResult);

        Map<String, String> videoShowScope = webClient.get()
                .uri(uriBuilder -> generateUri(uriBuilder, getVideoInfoQuery, "videos"))
                .headers(headers -> setCommonHeader(headers, googleAccessToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(rootNode ->
                            StreamSupport.stream(rootNode.path("items").spliterator(), false)
                                    .collect(Collectors.toMap(
                                            item -> item.get("id").asText(),
                                            item -> item.get("status").path("privacyStatus").asText()
                            ))
                )
                .block();

        fromGoogle.getVideoResources().forEach(videoResource -> {
            videoResource.setPrivacy(videoShowScope.getOrDefault(videoResource.getId(), "public"));
        });

        if (!"".equals(fromGoogle.getNextPageToken())) {
            redisService.save(VIDEO_REDIS_KEY + ":" + uuid, fromGoogle.getNextPageToken(), 30, TimeUnit.MINUTES);
            fromGoogle.setIsLast(false);
        } else {
            redisService.remove(VIDEO_REDIS_KEY + ":" + uuid);
            fromGoogle.setIsLast(true);
        }

        return GetVideosDto.Response.builder()
                .isLast(fromGoogle.getIsLast() ? "Y" : "N")
                .items(fromGoogle.getVideoResources())
                .build();
    }

    public GetCommentsDto.Response getCommentsByChannelId(GetCommentsDto.Request request, Member member) throws Exception {
        UUID uuid = member.getId();
        String googleAccessToken = getGoogleAccessToken(uuid);
        String channelId = member.getChannelId();

        Map<String, Object> queries = generateDefaultQueries();
        queries.put("part", "snippet,replies");
        queries.put("allThreadsRelatedToChannelId", channelId);
        queries.put("maxResults", request.getTake() == null ? VIDEO_MAX_RESULT : request.getTake());
        if (request.getPage() != 1) putNextPageTokenQuery(queries, CHANNEL_COMMENT_REDIS_KEY, uuid);

        return generateCommentResponseDto(queries, uuid, googleAccessToken);
    }

    public GetCommentsDto.Response getCommentsByVideoId(GetCommentsDto.Request request, String videoId, Member member) throws Exception {
        UUID uuid = member.getId();
        String googleAccessToken = getGoogleAccessToken(uuid);

        Map<String, Object> queries = generateDefaultQueries();
        queries.put("part", "snippet,replies");
        queries.put("videoId", videoId);
        queries.put("maxResults", request.getTake() == null ? COMMENT_MAX_RESULT : request.getTake());
        if (request.getPage() != 1) putNextPageTokenQuery(queries, VIDEO_COMMENT_REDIS_KEY, uuid);

        return generateCommentResponseDto(queries, uuid, googleAccessToken);
    }

    private GetCommentsDto.Response generateCommentResponseDto(Map<String, Object> queries, UUID uuid, String googleAccessToken) {
        List<PredictionInput> predictionInputs = fetchPredictTargetComments(queries, uuid, googleAccessToken, VIDEO_COMMENT_REDIS_KEY);

        CommentPredictDto.Request predictRequest = CommentPredictDto.Request.builder().items(predictionInputs).build();
        List<PredictionResponse> predictResponse = predictCommentsRequest(predictRequest, predictionInputs);

        predictResponse = generateHierarchyCommentThread(predictResponse);

        Object nextToken = redisService.get(VIDEO_COMMENT_REDIS_KEY + ":" + uuid);
        return GetCommentsDto.Response.builder()
                .items(predictResponse)
                .isLast(nextToken == null ? "Y" : "N")
                .build();
    }

    public void updateModerationAndAuthorBan(DeleteCommentsDto.Request request, UUID uuid) throws Exception {
        String googleAccessToken = getGoogleAccessToken(uuid);
        Map<String, Object> queries = generateDefaultQueries();

        // Remove Comment + Author Ban
        if (request.getAuthorBanComments() != null && !request.getAuthorBanComments().isEmpty()) {
            queries.put("id", request.getAuthorBanComments());
            queries.put("moderationStatus", "rejected");
            queries.put("banAuthor", true);
        }
        // Just Remove Comment
        if (request.getJustDeleteComments() != null && !request.getJustDeleteComments().isEmpty()) {
            queries.put("id", request.getJustDeleteComments());
            queries.put("moderationStatus", "heldForReview");
        }
        webClient.post()
                .uri(uriBuilder -> generateUri(uriBuilder, queries, "comments/setModerationStatus"))
                .headers(headers -> setCommonHeader(headers, googleAccessToken))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    private Map<String, Object> generateDefaultQueries() {
        Map<String, Object> queries = new HashMap<>();

        queries.put("key", apiKey);
        return queries;
    }

    private void putNextPageTokenQuery(Map<String, Object> queries, String redisKeyPrefix, UUID uuid) {
        String nextToken = (String) redisService.get(redisKeyPrefix + ":" + uuid);
        if (nextToken != null) queries.put("pageToken", nextToken);
    }

    private Mono<String> generateChannelId(JsonNode rootNode) {
        ArrayNode items = (ArrayNode) rootNode.path("items");
        String channelId = items.get(0).path("id").asText("");
        return Mono.just(channelId);
    }

    private Mono<YoutubeAccountDetail> generateYoutubeChannelDetail(JsonNode rootNode) {
        JsonNode items = rootNode.path("items");
        JsonNode channel = items.isArray() && !items.isEmpty() ? items.get(0) : NullNode.getInstance();
        String channelId = channel.path("id").asText("");
        String uploadsPlaylistId = channel.path("contentDetails").path("relatedPlaylists").path("uploads").asText("");

        return Mono.just(YoutubeAccountDetail.builder()
                .channelId(channelId)
                .playlistId(uploadsPlaylistId)
                .build());
    }

    private Mono<GetVideosDto.FromGoogle> generateVideosResponse(JsonNode rootNode, boolean fromSearch) {
        ArrayNode items = (ArrayNode) rootNode.path("items");

        List<YoutubeVideo> resultVideoResources = StreamSupport.stream(items.spliterator(), false)
                        .map(item -> {
                            JsonNode snippet = item.path("snippet");
                            String publishedAt = snippet.path("publishedAt").asText();

                            ZonedDateTime zonedDateTime = ZonedDateTime.parse(publishedAt);

                            String videoIdPath = fromSearch ? "id" : "resourceId";
                            String videoId = snippet.path(videoIdPath).path("videoId").asText("");
                            String videoTitle = snippet.path("title").asText("");
                            String thumbnailUrl = snippet.path("thumbnails").path("medium").path("url").asText("");
                            String description = snippet.path("description").asText("");

                            return YoutubeVideo.builder()
                                    .id(videoId)
                                    .title(videoTitle)
                                    .thumbnail(thumbnailUrl)
                                    .description(description)
                                    .publishedAt(zonedDateTime.toLocalDateTime())
                                    .build();
                        }).toList();

        GetVideosDto.FromGoogle fromGoogle = GetVideosDto.FromGoogle.builder()
                .videoResources(resultVideoResources)
                .nextPageToken(rootNode.path("nextPageToken").asText(""))
                .build();

        return Mono.just(fromGoogle);
    }

    private List<PredictionInput> fetchPredictTargetComments(Map<String, Object> queries,
                                                             UUID uuid,
                                                             String googleAccessToken,
                                                             String redisKey) {
        List<PredictionInput> predictionInputs = new ArrayList<>();
        for (int step = 0; step < STEP; step++) {
            GetCommentsDto.FromGoogle fromGoogle = webClient.get()
                    .uri(uriBuilder -> generateUri(uriBuilder, queries, "commentThreads"))
                    .headers(headers -> setCommonHeader(headers, googleAccessToken))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), response -> Mono.empty())
                    .bodyToMono(JsonNode.class)
                    .onErrorResume(e -> Mono.just(JsonNodeFactory.instance.objectNode()))
                    .flatMap(node -> generateCommentsResponseFromGoogle(node, googleAccessToken))
                    .block();

            predictionInputs.addAll(
                    fromGoogle.getComments().stream().map(comment ->
                            PredictionInput.builder()
                                    .profileImage(comment.getProfileImage())
                                    .id(comment.getId())
                                    .comment(comment.getComment())
                                    .nickname(comment.getNickname())
                                    .build()
                    ).toList()
            );

            if (!fromGoogle.getNextPageToken().isEmpty()) {
                queries.put("pageToken", fromGoogle.getNextPageToken());
                redisService.save(redisKey + ":" + uuid, fromGoogle.getNextPageToken(), 30, TimeUnit.MINUTES);
            } else {
                redisService.remove(redisKey + ":" + uuid);
                break;
            }
        }

        return predictionInputs;
    }


    private Mono<GetCommentsDto.FromGoogle> generateCommentsResponseFromGoogle(JsonNode rootPath, String googleAccessToken) {
        JsonNode pageInfo = rootPath.path("pageInfo");
        String nextPageToken = pageInfo.path("nextPageToken").asText("");

        JsonNode items = rootPath.path("items");

        List<YoutubeComment> comments = new ArrayList<>();
        List<Mono<List<YoutubeComment>>> additionalRepliesMonos = new ArrayList<>();

        if (items.isArray()) {
            for (JsonNode item : items) {
                JsonNode parent = item.path("snippet").path("topLevelComment");
                comments.add(generateYoutubeCommentSnippetValue(parent));

                JsonNode replies = item.path("replies").path("comments");
                if (replies.isArray()) {
                    int totalReplyCount = item.path("snippet").path("totalReplyCount").asInt();
                    if (totalReplyCount <= 5) {
                        replies.forEach(reply -> comments.add(generateYoutubeCommentSnippetValue(reply)));
                    } else {
                        String parentCommentId = parent.path("id").asText();
                        additionalRepliesMonos.add(getRepliesMono(parentCommentId, googleAccessToken));
                    }
                }
            }
        }

        if (additionalRepliesMonos.isEmpty()) {
            return Mono.just(GetCommentsDto.FromGoogle.builder()
                    .comments(comments)
                    .nextPageToken(nextPageToken)
                    .build()
            );
        }
        return Flux.concat(additionalRepliesMonos)
                .collectList()
                .map(listOfReplies -> {
                    listOfReplies.forEach(comments::addAll);
                    return GetCommentsDto.FromGoogle.builder()
                            .comments(comments)
                            .nextPageToken(nextPageToken)
                            .build();
                });
    }

    private YoutubeComment generateYoutubeCommentSnippetValue(JsonNode node) {
        JsonNode snippet = node.path("snippet");
        return YoutubeComment.builder()
                .id(node.path("id").asText())
                .nickname(snippet.path("authorDisplayName").asText())
                .comment(snippet.path("textOriginal").asText())
                .profileImage(snippet.path("authorProfileImageUrl").asText())
                .build();
    }

    private Mono<List<YoutubeComment>> getRepliesMono(String parentCommentId, String googleAccessToken) {
        List<YoutubeComment> replies = new ArrayList<>();
        Map<String, Object> queries = new HashMap<>();
        queries.put("part", "snippet");
        queries.put("maxResults", 100);
        queries.put("parentId", parentCommentId);

        return fetchRepliesPageMono(queries, googleAccessToken, replies);
    }

    private Mono<List<YoutubeComment>> fetchRepliesPageMono(Map<String, Object> queries, String googleAccessToken, List<YoutubeComment> accumulatedReplies) {
        return webClient.get()
                .uri(uriBuilder -> generateUri(uriBuilder, queries, "comments"))
                .headers(headers -> setCommonHeader(headers, googleAccessToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(node -> {
                    JsonNode replies = node.path("items");
                    if (replies.isArray())
                        for (JsonNode reply : replies) accumulatedReplies.add(generateYoutubeCommentSnippetValue(reply));

                    String nextPageToken = node.path("nextPageToken").asText("");
                    if (nextPageToken.isEmpty()) return Mono.just(accumulatedReplies);
                    else {
                        queries.put("pageToken", nextPageToken);
                        return fetchRepliesPageMono(queries, googleAccessToken, accumulatedReplies);
                    }
                });
    }

    private List<PredictionResponse> predictCommentsRequest(CommentPredictDto.Request predictRequest, List<PredictionInput> predictionInputs) {
        CommentPredictDto.Response predictResponse = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme(predictServerProperties.getScheme())
                        .host(predictServerProperties.getHost())
                        .port(predictServerProperties.getPort())
                        .path("/predict")
                        .build()
                )
                .bodyValue(predictRequest)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(this::generatePredictedResponse)
                .block();

        List<PredictionOutput> predictionOutputs = predictResponse.getResults();

        List<PredictionResponse> responsePredictedItems = new ArrayList<>();
        for (int i = 0; i < predictionInputs.size(); i++) {
            PredictionOutput predictionOutput = predictionOutputs.get(i);
//            if ("정상".equals(predictionOutput.getCommentPredicted())
//                    && "정상".equals(predictionOutput.getNicknamePredicted())
//            ) continue;

            PredictionInput predictionInput = predictionInputs.get(i);

            responsePredictedItems.add(
                    PredictionResponse.builder()
                            .id(predictionOutput.getId())
                            .profileImage(predictionInput.getProfileImage())
                            .comment(predictionInput.getComment())
                            .nickname(predictionInput.getNickname())
                            .commentPredict(predictionOutput.getCommentPredicted())
                            .nicknamePredict(predictionOutput.getNicknamePredicted())
                            .commentProb(predictionOutput.getCommentProb())
                            .nicknameProb(predictionOutput.getNicknameProb())
                            .build()
            );
        }

        return responsePredictedItems;
    }

    private List<PredictionResponse> generateHierarchyCommentThread(List<PredictionResponse> items) {
        Map<String, CommentThreadPredictionResult> commentThreadMap = generateCommentThreads(items);
        return convertResponseItemMapToList(commentThreadMap);
    }

    private Map<String, CommentThreadPredictionResult> generateCommentThreads(List<PredictionResponse> items) {
        Map<String, CommentThreadPredictionResult> commentThreadMap = new HashMap<>();
        int hardcodedRootIdLength = "UgxZ9ofN5JQkAVF9bfJ4AaABAg".length();
        for (PredictionResponse item : items) {
            String id = item.getId();
            String rootId = id.split("\\.")[0];

            CommentThreadPredictionResult rootCommentThread = commentThreadMap.computeIfAbsent(rootId, k -> new CommentThreadPredictionResult());
            if (id.length() == hardcodedRootIdLength) rootCommentThread.setTopLevelComment(item);
            else rootCommentThread.getReplies().add(item);
        }
        return commentThreadMap;
    }

    private List<PredictionResponse> convertResponseItemMapToList(Map<String, CommentThreadPredictionResult> commentThreadMap) {
        return commentThreadMap.entrySet().stream()
                .flatMap(entry -> {
                    CommentThreadPredictionResult commentThread = entry.getValue();
                    List<PredictionResponse> threadItems = new ArrayList<>();
                    PredictionResponse topLevelItem = commentThread.getTopLevelComment();
                    if (topLevelItem != null) {
                        topLevelItem.setIsTopLevel(true);
                        threadItems.add(topLevelItem);
                    }

                    threadItems.addAll(commentThread.getReplies().stream()
                            .map(reply -> {
                                reply.setIsTopLevel(false);
                                return reply;
                            }).toList());
                    return threadItems.stream();
                }).toList();
    }

    private Mono<CommentPredictDto.Response> generatePredictedResponse(JsonNode rootPath) {
        ArrayNode predictedItems = (ArrayNode) rootPath.path("items");
        List<PredictionOutput> resultItems = StreamSupport.stream(predictedItems.spliterator(), false)
                        .map(item -> {
                            List<Float> nicknameProb = generateListFromArrayNode((ArrayNode) item.path("nickname_predicted_prob"), node -> (float) node.asDouble());
                            List<Float> commentProb = generateListFromArrayNode((ArrayNode) item.path("comment_predicted_prob"), node -> (float) node.asDouble());
                            return PredictionOutput.builder()
                                    .id(item.get("id").asText())
                                    .commentPredicted(item.get("comment_predicted").asText())
                                    .nicknamePredicted(item.get("nickname_predicted").asText())
                                    .commentProb(commentProb)
                                    .nicknameProb(nicknameProb)
                                    .build();
                        }).toList();

        return Mono.just(CommentPredictDto.Response.builder().results(resultItems).build());
    }

    private <T> List<T> generateListFromArrayNode(ArrayNode arrayNode, Function<JsonNode, T> mapper) {
        return StreamSupport.stream(arrayNode.spliterator(), false)
                .map(mapper)
                .collect(Collectors.toList());
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
