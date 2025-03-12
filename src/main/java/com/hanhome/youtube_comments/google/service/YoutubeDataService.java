package com.hanhome.youtube_comments.google.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
        String channelId = member.getChannelId();

        String googleAccessToken = getGoogleAccessToken(uuid);

        Map<String, Object> queries = generateDefaultQueries();
        queries.put("part", "snippet,replies");
        queries.put("allThreadsRelatedToChannelId", channelId);
        queries.put("maxResults", request.getTake() == null ? VIDEO_MAX_RESULT : request.getTake());
        if (request.getPage() != 1) putNextPageTokenQuery(queries, CHANNEL_COMMENT_REDIS_KEY, uuid);

        List<PredictTargetItem> predictTargetItems = fetchPredictTargetComments(queries, uuid, googleAccessToken, CHANNEL_COMMENT_REDIS_KEY);

        CommentPredictDto.Request predictRequest = new CommentPredictDto.Request(predictTargetItems);
        List<PredictResponseItem> predictResponse = predictCommentsRequest(predictRequest, predictTargetItems);

        predictResponse = generateHierarchyCommentThread(predictResponse);

        Object nextToken = redisService.get(CHANNEL_COMMENT_REDIS_KEY + ":" + uuid);
        return GetCommentsDto.Response.builder()
                .items(predictResponse)
                .isLast(nextToken == null ? "Y" : "N")
                .build();
    }

    public GetCommentsDto.Response getCommentsByVideoId(GetCommentsDto.Request request, String videoId, Member member) throws Exception {
        UUID uuid = member.getId();
        String googleAccessToken = getGoogleAccessToken(uuid);

        Map<String, Object> queries = generateDefaultQueries();
        queries.put("part", "snippet,replies");
        queries.put("videoId", videoId);
        queries.put("maxResults", request.getTake() == null ? COMMENT_MAX_RESULT : request.getTake());
        if (request.getPage() != 1) putNextPageTokenQuery(queries, VIDEO_COMMENT_REDIS_KEY, uuid);

        List<PredictTargetItem> predictTargetItems = fetchPredictTargetComments(queries, uuid, googleAccessToken, VIDEO_COMMENT_REDIS_KEY);

        CommentPredictDto.Request predictRequest = new CommentPredictDto.Request(predictTargetItems);
        List<PredictResponseItem> predictResponse = predictCommentsRequest(predictRequest, predictTargetItems);

        predictResponse = generateHierarchyCommentThread(predictResponse);

        Object nextToken = redisService.get(VIDEO_COMMENT_REDIS_KEY + ":" + uuid);
        return GetCommentsDto.Response.builder()
                .items(predictResponse)
                .isLast(nextToken == null ? "Y" : "N")
                .build();
    }

    public void updateModerationAndAuthorBan(DeleteCommentsDto.Request request, UUID uuid) throws Exception {
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
            justRemoveQueries.put("moderationStatus", "heldForReview");

            webClient.post()
                    .uri(uriBuilder -> generateUri(uriBuilder, justRemoveQueries, "comments/setModerationStatus"))
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
        ArrayNode items = (ArrayNode) rootNode.path("items");

        JsonNode channel = items.get(0);
        String channelId = channel.path("id").asText();
        String uploadsPlaylistId = channel.path("contentDetails").path("relatedPlaylists").path("uploads").asText("");

        YoutubeAccountDetail detail = YoutubeAccountDetail.builder()
                .channelId(channelId)
                .playlistId(uploadsPlaylistId)
                .build();
        return Mono.just(detail);
    }

    private Mono<GetVideosDto.FromGoogle> generateVideosResponse(JsonNode rootNode, boolean fromSearch) {
        List<YoutubeVideo> resultVideoResources = new ArrayList<>();
        ArrayNode items = (ArrayNode) rootNode.path("items");
        items.forEach(item -> {
            JsonNode snippet = item.path("snippet");
            String publishedAt = snippet.path("publishedAt").asText();
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(publishedAt);
            String videoIdPath = fromSearch ? "id" : "resourceId";
            YoutubeVideo resource =
                    YoutubeVideo.builder()
                            .id(snippet.path(videoIdPath).path("videoId").asText(""))
                            .title(snippet.path("title").asText(""))
                            .thumbnail(snippet.path("thumbnails").path("medium").path("url").asText(""))
                            .description(snippet.path("description").asText(""))
                            .publishedAt(zonedDateTime.toLocalDateTime())
                            .build();

            resultVideoResources.add(resource);
        });

        GetVideosDto.FromGoogle fromGoogle = GetVideosDto.FromGoogle.builder()
                .videoResources(resultVideoResources)
                .nextPageToken(rootNode.path("nextPageToken").asText(""))
                .build();

        return Mono.just(fromGoogle);
    }

    private List<PredictTargetItem> fetchPredictTargetComments(Map<String, Object> queries,
                                                               UUID uuid,
                                                               String googleAccessToken,
                                                               String redisKey) {
        List<PredictTargetItem> predictTargetItems = new ArrayList<>();
        for (int step = 0; step < STEP; step++) {
            GetCommentsDto.FromGoogle fromGoogle = webClient.get()
                    .uri(uriBuilder -> generateUri(uriBuilder, queries, "commentThreads"))
                    .headers(headers -> setCommonHeader(headers, googleAccessToken))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .flatMap(node -> generateCommentsResponseFromGoogle(node, googleAccessToken))
                    .block();

            predictTargetItems.addAll(
                    fromGoogle.getComments().stream().map((comment) ->
                            PredictTargetItem.builder()
                                    .profileImage(comment.getProfileImage())
                                    .id(comment.getId())
                                    .comment(comment.getComment())
                                    .nickname(comment.getNickname()
                            ).build()
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

        return predictTargetItems;
    }


    private Mono<GetCommentsDto.FromGoogle> generateCommentsResponseFromGoogle(JsonNode rootPath, String googleAccessToken) {
        JsonNode pageInfo = rootPath.path("pageInfo");
        String nextPageToken = pageInfo.path("nextPageToken").asText("");

        ArrayNode items = (ArrayNode) rootPath.path("items");

        List<YoutubeComment> comments = new ArrayList<>();
        List<Mono<List<YoutubeComment>>> additionalRepliesMonos = new ArrayList<>();

        for (JsonNode item : items) {
            JsonNode parent = item.path("snippet").path("topLevelComment");
            comments.add(generateYoutubeCommentSnippetValue(parent));

        }
        items.forEach(item -> {
            JsonNode parent = item.path("snippet").path("topLevelComment");
            comments.add(generateYoutubeCommentSnippetValue(parent));

            JsonNode commentsNode = item.path("replies").path("comments");
            ArrayNode replies = commentsNode.isArray() ? (ArrayNode) commentsNode : new ObjectMapper().createArrayNode();

            if (item.path("snippet").path("totalReplyCount").asInt() <= 5) {
                for (JsonNode reply : replies) {
                    comments.add(generateYoutubeCommentSnippetValue(reply));
                }
            } else {
                String parentCommentId = parent.path("id").asText();
                additionalRepliesMonos.add(getRepliesMono(parentCommentId, googleAccessToken));
            }
        });

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
                    ArrayNode replies = (ArrayNode) node.path("items");
                    for (JsonNode reply : replies) accumulatedReplies.add(generateYoutubeCommentSnippetValue(reply));

                    String nextPageToken = node.path("nextPageToken").asText("");
                    if (nextPageToken.isEmpty()) return Mono.just(accumulatedReplies);
                    else {
                        queries.put("pageToken", nextPageToken);
                        return fetchRepliesPageMono(queries, googleAccessToken, accumulatedReplies);
                    }
                });
    }

    private List<PredictResponseItem> predictCommentsRequest(CommentPredictDto.Request predictRequest, List<PredictTargetItem> predictTargetItems) {
        List<PredictResultItem> predictResultItems = webClient.post()
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

        List<PredictResponseItem> responsePredictedItems = new ArrayList<>();
        for (int i = 0; i < predictTargetItems.size(); i++) {
            PredictResultItem predictResponse = predictResultItems.get(i);
//            if ("정상".equals(predictResponse.getCommentPredicted())
//                    && "정상".equals(predictResponse.getNicknamePredicted())
//            ) continue;

            PredictTargetItem predictTargetItem = predictTargetItems.get(i);

            responsePredictedItems.add(
                    PredictResponseItem.builder()
                            .id(predictResponse.getId())
                            .profileImage(predictTargetItem.getProfileImage())
                            .comment(predictTargetItem.getComment())
                            .nickname(predictTargetItem.getNickname())
                            .commentPredict(predictResponse.getCommentPredicted())
                            .nicknamePredict(predictResponse.getNicknamePredicted())
                            .commentProb(predictResponse.getCommentProb())
                            .nicknameProb(predictResponse.getNicknameProb())
                            .build()
            );
        }

        return responsePredictedItems;
    }

    private List<PredictResponseItem> generateHierarchyCommentThread(List<PredictResponseItem> items) {
        Map<String, PredictedCommentThread> commentThreadMap = generateCommentThreads(items);
        return convertResponseItemMapToList(commentThreadMap);
    }

    private Map<String, PredictedCommentThread> generateCommentThreads(List<PredictResponseItem> items) {
        Map<String, PredictedCommentThread> commentThreadMap = new HashMap<>();
        int hardcodedRootIdLength = "UgxZ9ofN5JQkAVF9bfJ4AaABAg".length();
        for (PredictResponseItem item : items) {
            String id = item.getId();
            String rootId = id.split("\\.")[0];

            PredictedCommentThread rootCommentThread = commentThreadMap.get(rootId);
            if (id.length() == hardcodedRootIdLength) {
                if (rootCommentThread == null) {
                    rootCommentThread = new PredictedCommentThread();
                    commentThreadMap.put(rootId, rootCommentThread);
                }
                rootCommentThread.setTopLevelComment(item);
            } else {
                if (rootCommentThread == null) {
                    rootCommentThread = new PredictedCommentThread();
                    commentThreadMap.put(rootId, rootCommentThread);
                }
                rootCommentThread.getReplies().add(item);
            }
        }
        return commentThreadMap;
    }

    private List<PredictResponseItem> convertResponseItemMapToList(Map<String, PredictedCommentThread> commentThreadMap) {
        List<PredictResponseItem> results = new ArrayList<>();
        for (Map.Entry<String, PredictedCommentThread> entry : commentThreadMap.entrySet()) {
            PredictedCommentThread commentThread = entry.getValue();

            PredictResponseItem topLevelItem = commentThread.getTopLevelComment();
            if (topLevelItem != null) {
                topLevelItem.setIsTopLevel(true);
                results.add(topLevelItem);
            }

            results.addAll(commentThread.getReplies().stream()
                    .peek(reply -> reply.setIsTopLevel(false))
                    .toList());
        }
        return results;
    }

    private Mono<List<PredictResultItem>> generatePredictedResponse(JsonNode rootPath) {
        List<PredictResultItem> resultItems = new ArrayList<>();
        ArrayNode predictedItems = (ArrayNode) rootPath.path("items");
        predictedItems.forEach((item) -> {
            List<Float> nicknameProb = generateListFromArrayNode((ArrayNode) item.path("nickname_predicted_prob"), node -> (float) node.asDouble());
            List<Float> commentProb = generateListFromArrayNode((ArrayNode) item.path("comment_predicted_prob"), node -> (float) node.asDouble());

            resultItems.add(
                    PredictResultItem.builder()
                        .id(item.get("id").asText())
                        .commentPredicted(item.get("comment_predicted").asText())
                        .nicknamePredicted(item.get("nickname_predicted").asText())
                        .commentProb(commentProb)
                        .nicknameProb(nicknameProb)
                        .build()
            );
        });

        return Mono.just(resultItems);
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
