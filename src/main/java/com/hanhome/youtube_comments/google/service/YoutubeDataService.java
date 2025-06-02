package com.hanhome.youtube_comments.google.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hanhome.youtube_comments.common.response.PredictCommonResponse;
import com.hanhome.youtube_comments.google.dto.*;
import com.hanhome.youtube_comments.google.exception.YoutubeAccessForbiddenException;
import com.hanhome.youtube_comments.google.object.*;
import com.hanhome.youtube_comments.google.object.predict.PredictionCombinedResource;
import com.hanhome.youtube_comments.google.object.predict.PredictionResponse;
import com.hanhome.youtube_comments.google.object.predict.PredictionResultResource;
import com.hanhome.youtube_comments.google.object.predict.PredictionInputResource;
import com.hanhome.youtube_comments.google.object.youtube_data_api.comment.CommentListResponse;
import com.hanhome.youtube_comments.google.object.youtube_data_api.comment.CommentResource;
import com.hanhome.youtube_comments.google.object.youtube_data_api.comment.CommentSnippetResource;
import com.hanhome.youtube_comments.google.object.youtube_data_api.comment_thread.*;
import com.hanhome.youtube_comments.google.object.youtube_data_api.playlist_items.PlaylistItemListResponse;
import com.hanhome.youtube_comments.google.object.youtube_data_api.playlist_items.PlaylistItemSnippetResource;
import com.hanhome.youtube_comments.member.entity.Member;
import com.hanhome.youtube_comments.member.object.YoutubeAccountDetail;
import com.hanhome.youtube_comments.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class YoutubeDataService {
    private final RedisService redisService;
    private final RenewGoogleTokenService googleTokenService;
    private final PredictServerProperties predictServerProperties;
    private final ObjectMapper objectMapper;

    private final WebClient webClient = WebClient.create();
    private final static int COMMENT_THREAD_MAX_REPLY = 5;
    private final static int VIDEO_MAX_RESULT = 50;
    private final static int COMMENT_MAX_RESULT = 100;
    private final static String VIDEO_REDIS_KEY = "NEXT_VIDEO";
    private final static String VIDEO_COMMENT_REDIS_KEY = "NEXT_COMMENT_V";

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
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    if (response.statusCode() == HttpStatus.FORBIDDEN) {
                        return response.bodyToMono(String.class)
                                .flatMap(errBody -> Mono.error(new YoutubeAccessForbiddenException("YouTube 권한 오류: " + errBody)));
                    }
                    return response.bodyToMono(String.class)
                            .flatMap(errBody -> Mono.error(new RuntimeException("비정상적인 오류 발생: " + errBody)));
                })
                .bodyToMono(JsonNode.class)
                .onErrorResume(YoutubeAccessForbiddenException.class, ex -> {
                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode rootNode = mapper.createObjectNode();

                    rootNode.set("items", mapper.createArrayNode());
                    return Mono.just(rootNode);
                })
                .flatMap(this::generateYoutubeChannelDetail)
                .block();
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

    public GetVideosDto.Response getVideosByPlaylist(GetVideosDto.Request request, Member member) throws Exception {
        UUID uuid = member.getId();
        String playlistId = member.getPlaylistId();
        String googleAccessToken = getGoogleAccessToken(uuid);

        if (playlistId.isEmpty()) return GetVideosDto.Response.builder()
                .isLast("Y")
                .items(new ArrayList<>())
                .build();

        Map<String, Object> getVideoQuery = generateDefaultQueries();

        getVideoQuery.put("part", "snippet,status");
        getVideoQuery.put("playlistId", playlistId);

        int maxResult = request.getTake() == null ? VIDEO_MAX_RESULT : request.getTake();
        getVideoQuery.put("maxResults", maxResult);

        if (request.getPage() != 1) {
            putNextPageTokenOnQuery(getVideoQuery, VIDEO_REDIS_KEY, uuid);
        }

        PlaylistItemListResponse playlistItemListResponse = webClient.get()
                .uri(uriBuilder -> generateUri(uriBuilder, getVideoQuery, "playlistItems"))
                .headers(headers -> setCommonHeader(headers, googleAccessToken))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                    clientResponse.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                System.err.println("Error Response: " + errorBody);
                                return Mono.error(new RuntimeException("API Error: " + errorBody));
                            })
                )
                .bodyToMono(PlaylistItemListResponse.class)
                .block();

        assert playlistItemListResponse != null;
        List<YoutubeVideo> resultVideoResources = playlistItemListResponse.getItems().stream().map(item -> {
            PlaylistItemSnippetResource playlistItemSnippetResource = item.getSnippet();
            return YoutubeVideo.builder()
                    .privacy(item.getPrivacyStatus())
                    .id(playlistItemSnippetResource.getVideoId())
                    .title(playlistItemSnippetResource.getTitle())
                    .thumbnail(playlistItemSnippetResource.getThumbnail())
                    .description(playlistItemSnippetResource.getDescription())
                    .publishedAt(playlistItemSnippetResource.getPublishedAt().toLocalDateTime())
                    .build();
        }).toList();

        boolean isLast = false;
        String videoRedisKey = VIDEO_REDIS_KEY + ":" + uuid;
        String nextPageToken = playlistItemListResponse.getNextPageToken();
        if (nextPageToken != null) {
            redisService.save(videoRedisKey, nextPageToken, 30, TimeUnit.MINUTES);
        } else {
            redisService.remove(videoRedisKey);
            isLast = true;
        }

        return GetVideosDto.Response.builder()
                .isLast(isLast ? "Y" : "N")
                .items(resultVideoResources)
                .build();
    }

    public GetCommentsDto.Response getCommentsByVideoId(GetCommentsDto.Request request, String videoId, Member member) throws Exception {
        UUID uuid = member.getId();
        String channelId = member.getChannelId();
        String googleAccessToken = getGoogleAccessToken(uuid);

        Map<String, Object> queries = generateDefaultQueries();
        queries.put("part", "snippet,replies");
        queries.put("videoId", videoId);
        queries.put("maxResults", request.getTake() == null ? COMMENT_MAX_RESULT : request.getTake());
        queries.put("moderationStatus", "published");
        if (request.getPage() != 1) putNextPageTokenOnQuery(queries, VIDEO_COMMENT_REDIS_KEY, uuid);

        ExecutorService executor = Executors.newFixedThreadPool(10);

        List<CommentThreadMap> commentThreadMaps = getCommentThreadListResponse(queries, googleAccessToken);
        List<CompletableFuture<Object>> futures = commentThreadMaps.stream()
                .filter(commentThread -> commentThread.getReplies() == null)
                .map(commentThread ->
                        CompletableFuture.supplyAsync(() -> {
                            YoutubeComment topLevel = commentThread.getTopLevel();
                            List<YoutubeComment> result = getRepliesFromParentId(topLevel.getId(), googleAccessToken);
                            commentThread.setReplies(result);
                            return null;
                        }, executor)
                ).toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        List<PredictionCombinedResource> predictResults = requestPredictCommentsClass(commentThreadMaps, channelId)
                .stream()
                .filter(predictResult -> {
                            boolean nicknameNormal = "정상".equals(predictResult.getNicknamePredict());
                            boolean commentNormal = "정상".equals(predictResult.getCommentPredict());
                            return !nicknameNormal || !commentNormal;
                        }
                )
                .toList();

        savePredictedResult(videoId, predictResults);

        return GetCommentsDto.Response.builder()
                .predictCommonResponse(PredictCommonResponse.builder().code(200).message("success").build())
                .items(predictResults)
                .isLast("Y")
                .build();
    }

    private List<PredictionCombinedResource> requestPredictCommentsClass(List<CommentThreadMap> commentThreadMaps, String channelId) {
        List<YoutubeComment> flattedCommentThreadMaps = commentThreadMaps.stream()
                .flatMap(thread -> Stream.concat(Stream.of(thread.getTopLevel()), thread.getReplies().stream()))
                .filter(commentThread -> !channelId.equals(commentThread.getChannelId()))
                .toList();

        if (flattedCommentThreadMaps.isEmpty()) return Collections.emptyList();

        List<PredictionInputResource> predictionInputResources = flattedCommentThreadMaps.stream()
                .map(commentThread ->
                    PredictionInputResource.builder()
                            .nickname(commentThread.getNickname())
                            .comment(commentThread.getComment())
                            .build()
                )
                .toList();
        CommentPredictDto.Request predictionRequest = CommentPredictDto.Request.builder().items(predictionInputResources).build();

        PredictionResponse predictionResponse = webClient.post()
                .uri( uriBuilder -> uriBuilder
                        .scheme(predictServerProperties.getScheme())
                        .host(predictServerProperties.getHost())
                        .port(predictServerProperties.getPort())
                        .path("/predict")
                        .build()
                )
                .bodyValue(predictionRequest)
                .retrieve()
                .bodyToMono(PredictionResponse.class)
                .block();

        assert predictionResponse != null;
        List<PredictionResultResource> predictionResults = predictionResponse.getItems();

        return IntStream.range(0, predictionInputResources.size())
                .mapToObj(i -> new PredictionCombinedResource(
                        flattedCommentThreadMaps.get(i),
                        predictionResults.get(i)
                ))
                .toList();
    }

    private List<YoutubeComment> getRepliesFromParentId(String parentCommentId, String googleAccessToken) {
        List<YoutubeComment> replies = new ArrayList<>();
        Map<String, Object> queries = new HashMap<>();
        queries.put("part", "snippet");
        queries.put("maxResults", COMMENT_MAX_RESULT);
        queries.put("parentId", parentCommentId);

        while (true) {
            CommentListResponse commentListResponse = webClient.get()
                    .uri(uriBuilder -> generateUri(uriBuilder, queries, "comments"))
                    .headers(headers -> setCommonHeader(headers, googleAccessToken))
                    .retrieve()
                    .bodyToMono(CommentListResponse.class)
                    .block();
            assert commentListResponse != null;
            replies.addAll(commentListResponse.getItems().stream().map(this::convertCommentSnippetToCommentObject).toList());

            String nextPageToken = commentListResponse.getNextPageToken();
            if (nextPageToken == null) break;
            queries.put("pageToken", nextPageToken);
        }
        return replies;
    }

    // 아 몰랑 전부 가져와! 이러면 redis로 관리할 필요가 없어진다!
    private List<CommentThreadMap> getCommentThreadListResponse(Map<String, Object> queries, String googleAccessToken) {
        List<CommentThreadMap> result = new ArrayList<>();
        while (true) {
            CommentThreadListResponse commentThreadListResponse = webClient.get()
                    .uri(uriBuilder -> generateUri(uriBuilder, queries, "commentThreads"))
                    .headers(headers -> setCommonHeader(headers, googleAccessToken)) // access token이 없더라도 api key 전달으로 가능!
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.empty())
                    .bodyToMono(CommentThreadListResponse.class)
                    .block();

            assert commentThreadListResponse != null;
            List<CommentThreadMap> temp = Optional.ofNullable(commentThreadListResponse.getItems())
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(commentThreadResource -> {
                        CommentThreadSnippetResource threadSnippet = commentThreadResource.getSnippet();
                        CommentResource topLevelComment = threadSnippet.getTopLevelComment();
                        List<CommentResource> replies = Optional.ofNullable(commentThreadResource.getReplies())
                                .map(CommentThreadRepliesResource::getComments)
                                .orElse(Collections.emptyList());

                        YoutubeComment mappingTopLevelComment = convertCommentSnippetToCommentObject(topLevelComment);
                        List<YoutubeComment> mappingReplies = threadSnippet.getTotalReplyCount() <= COMMENT_THREAD_MAX_REPLY
                                ? replies.stream().map(this::convertCommentSnippetToCommentObject).toList()
                                : null;

                        return CommentThreadMap.builder()
                                .topLevel(mappingTopLevelComment)
                                .replies(mappingReplies)
                                .build();
                    }).toList();
            result.addAll(temp);

            String nextPageToken = commentThreadListResponse.getNextPageToken();
            if (nextPageToken == null) break;
            queries.put("pageToken", nextPageToken);
        }
        return result;
    }

    private YoutubeComment convertCommentSnippetToCommentObject(CommentResource commentResource) {
        CommentSnippetResource commentSnippet = commentResource.getSnippet();
        return YoutubeComment.builder()
                .id(commentResource.getId())
                .channelId(commentSnippet.getAuthorChannelId())
                .nickname(commentSnippet.getAuthorDisplayName())
                .profileImage(commentSnippet.getAuthorProfileImageUrl())
                .comment(commentSnippet.getTextOriginal())
                .build();
    }

    public void updateModerationAndAuthorBan(DeleteCommentsDto.Request request, Member member) throws Exception {
        String googleAccessToken = getGoogleAccessToken(member.getId());
        Map<String, Object> queries = generateDefaultQueries();

        // 댓글 삭제 - 작성자만 삭제 가능 https://developers.google.com/youtube/v3/guides/implementation/comments?hl=ko#comments-delete
        // 게시 상태 변경 - 채널 관리자가 관리 가능 https://developers.google.com/youtube/v3/guides/implementation/comments?hl=ko#comments-set-moderation-status
        queries.put("moderationStatus", "rejected");

        // Just Remove Comment
        if (request.getJustDeleteComments() != null && !request.getJustDeleteComments().isEmpty()) {
            List<DeleteCommentObject> targetDeleteComments = request.getJustDeleteComments().stream()
                    .filter(deleteComment -> !member.getChannelId().equals(deleteComment.getChannelId()))
                    .toList();

            if (targetDeleteComments.isEmpty()) return;

            saveUserDeleteRequest(request.getVideoId(), targetDeleteComments);

            String removeCommentQueries = targetDeleteComments.stream()
                    .map(DeleteCommentObject::getCommentId)
                    .collect(Collectors.joining(","));
            queries.put("id", removeCommentQueries);

            webClient.post()
                    .uri(uriBuilder -> generateUri(uriBuilder, queries, "comments/setModerationStatus"))
                    .headers(headers -> setCommonHeader(headers, googleAccessToken))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .onErrorResume(e -> {
                        System.out.println(e);
                        return null;
                    })
                    .block();
        }
    }

    private Map<String, Object> generateDefaultQueries() {
        Map<String, Object> queries = new HashMap<>();

        queries.put("key", apiKey);
        return queries;
    }

    private void putNextPageTokenOnQuery(Map<String, Object> queries, String redisKeyPrefix, UUID uuid) {
        String nextToken = (String) redisService.get(redisKeyPrefix + ":" + uuid);
        if (nextToken != null) queries.put("pageToken", nextToken);
    }

    private Mono<String> generateChannelId(JsonNode rootNode) {
        ArrayNode items = (ArrayNode) rootNode.path("items");
        String channelId = items.get(0).path("id").asText("");
        return Mono.just(channelId);
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

    private void savePredictedResult(String id, List<PredictionCombinedResource> responsePredictedItems) {
        if (!responsePredictedItems.isEmpty()) {
            String filePath = generateFilePath(id, "predict");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                for (PredictionCombinedResource item : responsePredictedItems) {
                    String formattedData = predictionFormatData(item);
                    writer.write(formattedData);
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveUserDeleteRequest(String id, List<DeleteCommentObject> justDeleteComments) {
        if (!justDeleteComments.isEmpty()) {
            String filePath = generateFilePath(id, "delete");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                for (DeleteCommentObject item : justDeleteComments) {
                    String formattedData = item.getCommentId();
                    writer.write(formattedData);
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String generateFilePath(String id, String type) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");
        String formattedDate = ZonedDateTime.now().format(formatter);
        Path dirPath = Paths.get("tmp", "result", id);
        if (!Files.exists(dirPath)) {
            try {
                Files.createDirectories(dirPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return dirPath.resolve(type + "-" + formattedDate + ".txt").toString();
    }

    private String predictionFormatData(PredictionCombinedResource item) {
        return String.format("%s - %s\n\t%s - %s\n\t%s\n\t%s - %s\n\t%s",
                item.getId(), item.getProfileImage(),
                item.getNickname(), item.getNicknamePredict(), item.getNicknameProb().toString(),
                item.getComment(), item.getCommentPredict(), item.getCommentProb().toString()
        );
    }

    private void printObjectPretty(Object obj) throws JsonProcessingException {
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj));
    }
}
