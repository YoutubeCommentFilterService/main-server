package com.hanhome.youtube_comments.google.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hanhome.youtube_comments.exception.*;
import com.hanhome.youtube_comments.google.dto.RenewGoogleTokenDto;
import com.hanhome.youtube_comments.google.object.youtube_data_api.token.RenewAccessTokenErrorResponse;
import com.hanhome.youtube_comments.google.object.youtube_data_api.token.RenewAccessTokenResponse;
import com.hanhome.youtube_comments.google.object.youtube_data_api.token.RequestErrorResponse;
import com.hanhome.youtube_comments.member.repository.MemberRepository;
import com.hanhome.youtube_comments.redis.service.RedisService;
import com.hanhome.youtube_comments.utils.AESUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class GoogleAPIService {
    private final RedisService redisService;
    private final MemberRepository memberRepository;
    private final AESUtil aesUtil;

    private final WebClient webClient = WebClient.create();
    private final String YOUTUBE_SCOPE = "youtube.force-ssl";

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${data.youtube.access-token}")
    private String redisGoogleAtKey;

    @Value("${data.youtube.api-key}")
    private String apiKey;

    public boolean hasYoutubeAccess(String googleAccessToken) {
        return Boolean.TRUE.equals(webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("www.googleapis.com")
                        .path("/oauth2/v1/tokeninfo")
                        .queryParam("access_token", googleAccessToken)
                        .build()
                )
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(jsonNode -> {
                        String[] scopes = jsonNode.get("scope").asText().split(" ");
                        boolean result = Arrays.stream(scopes).anyMatch(s -> s.endsWith(YOUTUBE_SCOPE));
                        return Mono.just(result);
                })
                .block());
    }

    public boolean hasYoutubeAccess(UUID uuid) {
        return memberRepository.findById(uuid).get().getHasYoutubeAccess();
    }

    public <T> T getObjectFromYoutubeDataAPI(
            HttpMethod httpMethod,
            String endpoint,
            Map<String, Object> queries,
            Class<T> toMono
    ) throws Exception {
        return getObjectFromYoutubeDataAPI(httpMethod, endpoint, queries, null, toMono, Mono::just, null);
    }

    public <T, R> R getObjectFromYoutubeDataAPI(
            HttpMethod httpMethod,
            String endpoint,
            Map<String, Object> queries,
            Class<T> toMono,
            Function<T, Mono<R>> bodyProcessor
    ) throws Exception {
        return getObjectFromYoutubeDataAPI(httpMethod, endpoint, queries, null, toMono, bodyProcessor, null);
    }

    public <T> T getObjectFromYoutubeDataAPI(
            HttpMethod httpMethod,
            String endpoint,
            Map<String, Object> queries,
            UUID uuid,
            Class<T> toMono
    ) throws Exception {
        return getObjectFromYoutubeDataAPI(httpMethod, endpoint, queries, uuid, toMono, Mono::just, null);
    }

    public <T, R> R getObjectFromYoutubeDataAPI(
            HttpMethod httpMethod,
            String endpoint,
            Map<String, Object> queries,
            UUID uuid,
            Class<T> toMono,
            Function<T, Mono<R>> bodyProcessor
    ) throws Exception {
        return getObjectFromYoutubeDataAPI(httpMethod, endpoint, queries, uuid, toMono, bodyProcessor, null);
    }

    public <T, R> R getObjectFromYoutubeDataAPI(
            HttpMethod httpMethod,
            String endpoint,
            Map<String, Object> queries,
            UUID uuid,
            Class<T> toMono,
            Function<T, Mono<R>> bodyProcessor,
            Object body
    ) throws Exception {
        queries.put("key", apiKey);

        try {
            return doRequest(httpMethod, endpoint, queries, uuid, toMono, bodyProcessor, body);
        } catch (GoogleInvalidGrantException e) {
            takeNewGoogleAccessToken(uuid);
            return doRequest(httpMethod, endpoint, queries, uuid, toMono, bodyProcessor, body);
        }
    }

    private <T, R> R doRequest(
            HttpMethod httpMethod,
            String endpoint,
            Map<String, Object> queries,
            UUID uuid,
            Class<T> toMono,
            Function<T, Mono<R>> bodyProcessor,
            Object body
    ) {
        WebClient.ResponseSpec responseSpec = generateRequest(uuid, httpMethod, endpoint, queries, body);

        return responseSpec
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> response.bodyToMono(RequestErrorResponse.class)
                                .flatMap(errorResponse -> {
                                        RequestErrorResponse.ErrorCode.ErrorStatus errorCode =
                                                errorResponse.getError().getErrors().stream()
                                                        .findFirst()
                                                        .orElse(new RequestErrorResponse.ErrorCode.ErrorStatus());
                                        String errorMessage = errorCode.getMessage();
                                        // reason을 기준으로 해볼까? 어느 API는 status가 있고 어느 API는 status가 없다
                                        return switch (errorCode.getReason()) {
                                            case "commentsDisabled" ->  // from youtube data api
                                                    Mono.<Throwable>error(new YoutubeVideoCommentDisabledException(errorMessage));
                                            case "authError" ->  // "invalid grant type"
                                                    Mono.<Throwable>error(new GoogleInvalidGrantException(errorMessage));
                                            case "insufficientPermissions" ->  // "plz check permissions!!!!"
                                                    Mono.<Throwable>error(new YoutubeAccessForbiddenException(errorMessage));
                                            case "notFound" ->
                                                    Mono.<Throwable>error(new RequestedEntityNotFoundException(errorMessage));
                                            default ->
                                                    Mono.<Throwable>error(new RuntimeException("Unknown Error: " + errorMessage));
                                        };
                                })
                )
                .bodyToMono(toMono)
                .flatMap(bodyProcessor)
                .block();
    }

    private WebClient.ResponseSpec generateRequest(
            UUID uuid,
            HttpMethod httpMethod,
            String endpoint,
            Map<String, Object> queries,
            Object body
    ) {
            WebClient.RequestBodySpec requestSpec = webClient.method(httpMethod)
                    .uri(uriBuilder -> generateUri(uriBuilder, endpoint, queries))
                    .headers(httpHeaders -> setCommonHeader(httpHeaders, uuid));

            if (body != null) return requestSpec.bodyValue(body).retrieve();
            return requestSpec.retrieve();
    }

    private URI generateUri(UriBuilder uriBuilder, String endpoint, Map<String, Object> queries) {
        uriBuilder.scheme("https")
                .host("www.googleapis.com")
                .path("/youtube/v3/" + endpoint)
                .build();

        queries.forEach(uriBuilder::queryParam);
        return uriBuilder.build();
    }


    private void setCommonHeader(HttpHeaders headers, UUID uuid) {
        if (uuid != null) {
            String googleAccessToken = (String) redisService.get(generateGoogleAccessTokenKey(uuid));
            headers.add("Authorization", "Bearer " + googleAccessToken);
            headers.add("Accept", "application/json");
        }
    }

    public String takeNewGoogleAccessToken(UUID uuid) throws Exception {
        URI uri = URI.create("https://oauth2.googleapis.com/token");

        String googleRefreshToken = memberRepository.findById(uuid)
                .map(m -> {
                    try {
                        return aesUtil.decrypt(m.getGoogleRefreshToken());
                    } catch (Exception e) {
                        throw new GoogleRefreshTokenNotFoundException("Google Refresh Token is Not Encrypted Type");
                    }
                })
                .orElseThrow(() -> new GoogleRefreshTokenNotFoundException("Google Refresh Token is Null"));

        RenewGoogleTokenDto.Request dto = RenewGoogleTokenDto.Request.builder()
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .grantType("refresh_token")
                        .refreshToken(googleRefreshToken)
                        .build();

        RenewAccessTokenResponse tokenResponse = webClient.post()
                .uri(uri)
                .bodyValue(dto)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(RenewAccessTokenErrorResponse.class)
                                .flatMap(error -> {
                                            System.out.println(error);
                                            return Mono.error(new RenewAccessTokenFailedException(error.getError()));
                                        })
                )
                .bodyToMono(RenewAccessTokenResponse.class)
                .block();

        redisService.save(generateGoogleAccessTokenKey(uuid), tokenResponse.getAccessToken(), tokenResponse.getExpiresIn(), TimeUnit.SECONDS);

        return tokenResponse.getAccessToken();
    }

    private String generateGoogleAccessTokenKey(UUID uuid) {
        return redisGoogleAtKey + ":" + uuid.toString();
    }
}
