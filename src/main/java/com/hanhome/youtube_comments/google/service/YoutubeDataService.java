package com.hanhome.youtube_comments.google.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.hanhome.youtube_comments.common.response.PredictCommonResponse;
import com.hanhome.youtube_comments.exception.RequestedEntityNotFoundException;
import com.hanhome.youtube_comments.google.dto.*;
import com.hanhome.youtube_comments.exception.RenewAccessTokenFailedException;
import com.hanhome.youtube_comments.exception.YoutubeAccessForbiddenException;
import com.hanhome.youtube_comments.exception.YoutubeVideoCommentDisabledException;
import com.hanhome.youtube_comments.google.object.*;
import com.hanhome.youtube_comments.google.object.predict.PredictionCombinedResource;
import com.hanhome.youtube_comments.google.object.predict.PredictionResponse;
import com.hanhome.youtube_comments.google.object.predict.PredictionResultResource;
import com.hanhome.youtube_comments.google.object.predict.PredictionInputResource;
import com.hanhome.youtube_comments.google.object.youtube_data_api.channel.ChannelContentDetailsResource;
import com.hanhome.youtube_comments.google.object.youtube_data_api.channel.ChannelListResponse;
import com.hanhome.youtube_comments.google.object.youtube_data_api.channel.ChannelSnippetResource;
import com.hanhome.youtube_comments.google.object.youtube_data_api.comment.CommentListResponse;
import com.hanhome.youtube_comments.google.object.youtube_data_api.comment.CommentResource;
import com.hanhome.youtube_comments.google.object.youtube_data_api.comment.CommentSnippetResource;
import com.hanhome.youtube_comments.google.object.youtube_data_api.comment_thread.*;
import com.hanhome.youtube_comments.google.object.youtube_data_api.playlist_items.PlaylistItemListResponse;
import com.hanhome.youtube_comments.google.object.youtube_data_api.playlist_items.PlaylistItemSnippetResource;
import com.hanhome.youtube_comments.google.object.youtube_data_api.video.HotVideoResponseField;
import com.hanhome.youtube_comments.google.object.youtube_data_api.video.VideoFlatMap;
import com.hanhome.youtube_comments.google.object.youtube_data_api.video.VideoListResponse;
import com.hanhome.youtube_comments.google.object.youtube_data_api.video_category.VideoCategoryFlatMap;
import com.hanhome.youtube_comments.google.object.youtube_data_api.video_category.VideoCategoryListResponse;
import com.hanhome.youtube_comments.google.object.youtube_data_api.video_category.VideoCategoryResource;
import com.hanhome.youtube_comments.member.entity.Member;
import com.hanhome.youtube_comments.member.object.YoutubeAccountDetail;
import com.hanhome.youtube_comments.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
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
    private final PredictServerProperties predictServerProperties;
    private final ObjectMapper objectMapper;
    private final LoggingPredictedService loggingPredictedService;
    private final GoogleAPIService googleAPIService;
    private final GeminiService geminiService;

    private final WebClient webClient = WebClient.create();
    private final static int COMMENT_THREAD_MAX_REPLY = 5;
    private final static int VIDEO_MAX_RESULT = 50;
    private final static int COMMENT_MAX_RESULT = 100;
    private final static String VIDEO_REDIS_KEY = "NEXT_VIDEO";

    @Value("${data.youtube.api-key}")
    private String apiKey;

    @Value("${data.youtube.access-token}")
    private String redisGoogleAtKey;

    @Value("${data.youtube.hot-video}")
    private String redisHotVideoKey;

    private String getGoogleAccessToken(UUID uuid) throws Exception {
        String googleAccessToken = (String) redisService.get(redisGoogleAtKey + ":" + uuid);
        return googleAccessToken == null ? googleAPIService.takeNewGoogleAccessToken(uuid) : googleAccessToken;
    }

    public YoutubeAccountDetail getYoutubeAccountDetail(String googleAccessToken) {
        Map<String, Object> queries = new HashMap<>();
        queries.put("key", apiKey);
        queries.put("part", "snippet,contentDetails");
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
                .bodyToMono(ChannelListResponse.class)
                .flatMap(this::generateYoutubeChannelDetail)
                .block()
                .get(0);
    }

    public YoutubeAccountDetail getYoutubeAccountDetail(UUID uuid) throws Exception {
        Map<String, Object> queries = new HashMap<>();
        queries.put("part", "snippet,contentDetails");
        queries.put("mine", true);

        return googleAPIService.getObjectFromYoutubeDataAPI(
                HttpMethod.GET,
                "channels",
                queries,
                uuid,
                ChannelListResponse.class,
                this::generateYoutubeChannelDetail
        ).get(0);
    }

    public List<YoutubeAccountDetail> getMultiYoutubeAccountDetail(List<String> channelIds) throws Exception {
        Map<String, Object> queries = new HashMap<>();
        queries.put("part", "snippet,contentDetails,statistics");
        queries.put("id", String.join(",", channelIds));

        return googleAPIService.getObjectFromYoutubeDataAPI(
                HttpMethod.GET,
                "channels",
                queries,
                ChannelListResponse.class,
                this::generateYoutubeChannelDetail
        );
    }

    private Mono<List<YoutubeAccountDetail>> generateYoutubeChannelDetail(ChannelListResponse channelListResponse) {
        return Mono.just(
                channelListResponse.getItems().stream().map(resource -> {
                    ChannelSnippetResource channelSnippetResource = resource.getSnippet();
                    ChannelContentDetailsResource channelContentDetailsResource = resource.getContentDetails();
                    return YoutubeAccountDetail.builder()
                            .channelId(resource.getId())
                            .playlistId(channelContentDetailsResource.getRelatedPlaylists().getUploads())
                            .channelHandler(channelSnippetResource.getCustomUrl())
                            .channelName(channelSnippetResource.getTitle())
                            .thumbnailUrl(channelSnippetResource.getThumbnail())
                            .subscriberCount(resource.getStatistics().getSubscriberCount())
                            .build();
                }).toList()
        );
    }

    public GetVideosDto.Response getVideosByPlaylist(GetVideosDto.Request request, Member member) throws Exception {
        UUID uuid = member.getId();
        String playlistId = member.getPlaylistId();

        if (!member.getHasYoutubeAccess()) return GetVideosDto.Response.builder()
                .isLast("Y")
                .items(new ArrayList<>())
                .build();

        Map<String, Object> queries = new HashMap<>();
        queries.put("part", "snippet,status");
        queries.put("playlistId", playlistId);
        queries.put("maxResults", VIDEO_MAX_RESULT);

        if (request.getPage() != 1) putNextPageTokenOnQuery(queries, VIDEO_REDIS_KEY, uuid);

        PlaylistItemListResponse playlistItemListResponse = googleAPIService.getObjectFromYoutubeDataAPI(
                HttpMethod.GET,
                "playlistItems",
                queries,
                uuid,
                PlaylistItemListResponse.class
        );

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

    public GetHotVideosDto.Response getHotVideosFromRedis() {
        return redisService.get(redisHotVideoKey, GetHotVideosDto.Response.class);
    }

    public GetHotVideosDto.Response getHotVideos() throws Exception {
        Set<String> channelIdSet = new HashSet<>();
        Map<String, HotVideoResponseField> flattedMap = new HashMap<>();
        Map<String, Object> queries = new HashMap<>();
        queries.put("chart", "mostPopular");
        queries.put("part", "snippet,statistics");
        queries.put("regionCode", "KR");
        queries.put("hl", "ko");
        queries.put("maxResults", 50);

        // 1. 전체 인급동
        queries.remove("videoCategoryId");
        appendDatas(queries, "0:전체", flattedMap, channelIdSet);

        // 2. 개별 카테고리 인급동
        List<VideoCategoryFlatMap> videoCategoryIds = getVideoCategoryId();
        for (VideoCategoryFlatMap category : videoCategoryIds) {
            try {
                queries.put("videoCategoryId", category.getId());
                appendDatas(queries, category.toString(), flattedMap, channelIdSet);
            } catch (RequestedEntityNotFoundException ignored) {}
        }

        Set<String> summarizeTargetVideos = new HashSet<>();
        // 인급동 카테고리 중 특정 카테고리만 요약 실행
        String[] summarizeCategories = { "뉴스/정치", "인물/블로그" };
        for (String category : summarizeCategories) {
            HotVideoResponseField field = flattedMap.get(category);
            if (field != null) {
                List<VideoFlatMap> flattedMaps = field.getItems();
                if (flattedMaps != null && !flattedMaps.isEmpty())
                    summarizeTargetVideos.addAll(
                            field.getItems().stream()
                                    .map(VideoFlatMap::getVideoId)
                                    .toList()
                    );
            }
        }

        Map<String, String> geminiOutputs  = geminiService.generateSummarizationVideoCaptions(summarizeTargetVideos.stream().toList());

        List<String> channelIdList = new ArrayList<>(channelIdSet);
        int batchSize = 50;
        Map<String, YoutubeAccountDetail.HandlerUrlMapper> accountDetails = IntStream.iterate(0, i -> i < channelIdList.size(), i -> i + batchSize)
                .mapToObj(i -> {
                    List<String> batch = channelIdList.subList(i, Math.min(i + batchSize, channelIdList.size()));
                    try {
                        return getMultiYoutubeAccountDetail(batch);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .flatMap(List::stream)
                .collect(Collectors.toMap(
                        YoutubeAccountDetail::getChannelId,
                        YoutubeAccountDetail::mapToHandlerUrl
                ));

        flattedMap.forEach((key, data) ->{
            data.getItems().forEach(flatMap -> {
                flatMap.setNonstaticField(accountDetails.get(flatMap.getChannelId()));
                String summarized = geminiOutputs.get(flatMap.getVideoId());
                if (summarized != null && !summarized.isEmpty()) {
                    flatMap.setSummarized(geminiOutputs.get(flatMap.getVideoId()));
                }
            });
        });

        String now = Instant.now().toString();
        return GetHotVideosDto.Response.builder()
                .baseTime(now)
                .itemMap(flattedMap)
                .build();
    }

    private void appendDatas(Map<String, Object> queries, String mapKey, Map<String, HotVideoResponseField> flattedMap, Set<String> channelIds) throws Exception {
        String[] splittedMapKey = mapKey.split(":");
        List<VideoFlatMap> flattedList = getHotVideosInCategory(queries);
        channelIds.addAll(flattedList.stream().map(VideoFlatMap::getChannelId).toList());
        HotVideoResponseField responseField = HotVideoResponseField.builder()
                        .key(Integer.parseInt(splittedMapKey[0]))
                        .items(flattedList)
                        .build();
        flattedMap.put(splittedMapKey[1], responseField);
    }

    private List<VideoFlatMap> getHotVideosInCategory(Map<String, Object> queries) throws Exception {
        String nextPageToken = null;
        List<VideoFlatMap> flatted = new ArrayList<>();
        do {
            VideoListResponse response = googleAPIService.getObjectFromYoutubeDataAPI(
                    HttpMethod.GET,
                    "videos",
                    queries,
                    VideoListResponse.class
            );
            nextPageToken = response.getNextPageToken();
            queries.put("pageToken", nextPageToken);
            flatted.addAll(response.getItems().stream().map(VideoFlatMap::new).toList());
        } while (nextPageToken != null);
        return flatted;
    }

    private List<VideoCategoryFlatMap> getVideoCategoryId() throws Exception {
        Map<String, Object> queries = new HashMap<>();
        queries.put("part", "snippet");
        queries.put("regionCode", "KR");
        queries.put("hl", "ko_KR");

        VideoCategoryListResponse response = googleAPIService.getObjectFromYoutubeDataAPI(
                HttpMethod.GET,
                "videoCategories",
                queries,
                VideoCategoryListResponse.class
        );

        List<VideoCategoryResource> list = response.getItems().stream().filter(item -> item.getSnippet().getAssignable()).toList();
        return list.stream().map(VideoCategoryFlatMap::new).toList();
    }

    public GetCommentsDto.Response getCommentsByVideoId(String videoId, Member member) throws Exception {
        UUID uuid = member.getId();
        String channelId = member.getChannelId();
        String googleAccessToken = getGoogleAccessToken(uuid);

        Map<String, Object> queries = new HashMap<>();
        queries.put("part", "snippet,replies");
        queries.put("videoId", videoId);
        queries.put("maxResults", COMMENT_MAX_RESULT);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CommentThreadMap> commentThreadMaps;

        try {
            commentThreadMaps = getCommentThreadListResponse(queries, googleAccessToken, uuid);
        } catch (YoutubeAccessForbiddenException | YoutubeVideoCommentDisabledException e) {
            return GetCommentsDto.Response.builder()
                    .predictCommonResponse(PredictCommonResponse.builder().code(403).message(e.getMessage()).build())
                    .items(List.of())
                    .isLast("Y")
                    .build();
        } catch (RenewAccessTokenFailedException e) {
            return GetCommentsDto.Response.builder()
                    .predictCommonResponse(PredictCommonResponse.builder().code(401).message(e.getMessage()).build())
                    .items(List.of())
                    .isLast("Y")
                    .build();
        }

        List<CompletableFuture<Object>> futures = commentThreadMaps.stream()
                .filter(commentThread -> commentThread.getReplies() == null)
                .map(commentThread ->
                        CompletableFuture.supplyAsync(() -> {
                            YoutubeComment topLevel = commentThread.getTopLevel();
                            List<YoutubeComment> result = null;
                            try {
                                result = getRepliesFromParentId(topLevel.getId(), googleAccessToken, uuid);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
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

        loggingPredictedService.savePredictLogging(videoId, predictResults);

        return GetCommentsDto.Response.builder()
                .predictCommonResponse(PredictCommonResponse.builder().code(200).message("success").build())
                .items(predictResults)
                .isLast("Y")
                .build();
    }

    private List<PredictionCombinedResource> requestPredictCommentsClass(List<CommentThreadMap> commentThreadMaps, String channelId) {
        List<YoutubeComment> flattedCommentThreadMaps = commentThreadMaps.stream()
                .flatMap(thread -> Stream.concat(Stream.of(thread.getTopLevel()), thread.getReplies().stream()))
//                .filter(commentThread -> !channelId.equals(commentThread.getChannelId()))
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

        // TODO: 추론 서버가 꺼져있는 경우 빈 배열로 처리 또는 Exception 던져서 에러 처리
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

    // 아 몰랑 전부 가져와! 이러면 redis로 관리할 필요가 없어진다!
    private List<CommentThreadMap> getCommentThreadListResponse(Map<String, Object> queries, String googleAccessToken, UUID uuid) throws Exception {
        List<CommentThreadMap> result = new ArrayList<>();
        while (true) {
            CommentThreadListResponse commentThreadListResponse = googleAPIService.getObjectFromYoutubeDataAPI(
                    HttpMethod.GET,
                    "commentThreads",
                    queries,
                    uuid,
                    CommentThreadListResponse.class
            );

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

    private List<YoutubeComment> getRepliesFromParentId(String parentCommentId, String googleAccessToken, UUID uuid) throws Exception {
        List<YoutubeComment> replies = new ArrayList<>();
        Map<String, Object> queries = new HashMap<>();
        queries.put("part", "snippet");
        queries.put("maxResults", COMMENT_MAX_RESULT);
        queries.put("parentId", parentCommentId);

        while (true) {
            CommentListResponse commentListResponse = googleAPIService.getObjectFromYoutubeDataAPI(
                    HttpMethod.GET,
                    "comments",
                    queries,
                    uuid,
                    CommentListResponse.class
            );
            replies.addAll(commentListResponse.getItems().stream().map(this::convertCommentSnippetToCommentObject).toList());

            String nextPageToken = commentListResponse.getNextPageToken();
            if (nextPageToken == null) break;
            queries.put("pageToken", nextPageToken);
        }
        return replies;
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
        UUID uuid = member.getId();

        // 댓글 삭제 - 작성자만 삭제 가능 https://developers.google.com/youtube/v3/guides/implementation/comments?hl=ko#comments-delete
        // 게시 상태 변경 - 채널 관리자가 관리 가능 https://developers.google.com/youtube/v3/guides/implementation/comments?hl=ko#comments-set-moderation-status
        Map<String, Object> queries = new HashMap<>();
        queries.put("moderationStatus", "rejected");

        // Just Remove Comment
        if (request.getJustDeleteComments() != null && !request.getJustDeleteComments().isEmpty()) {
            String videoId = request.getVideoId();
            List<DeleteCommentObject> targetDeleteComments = request.getJustDeleteComments();

            loggingPredictedService.saveDeletedLogging(videoId, targetDeleteComments);

            String removeCommentQueries = targetDeleteComments.stream()
                    .map(DeleteCommentObject::getCommentId)
                    .collect(Collectors.joining(","));
            queries.put("id", removeCommentQueries);

            googleAPIService.getObjectFromYoutubeDataAPI(
                    HttpMethod.POST,
                    "comments/setModerationStatus",
                    queries,
                    uuid,
                    Void.class
            );
        }
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

    private void printObjectPretty(Object obj) {
        try {
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            System.out.println("Runtime Error!");
        }
    }
}
