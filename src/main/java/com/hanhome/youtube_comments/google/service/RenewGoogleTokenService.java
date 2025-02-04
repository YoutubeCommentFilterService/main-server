package com.hanhome.youtube_comments.google.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hanhome.youtube_comments.google.dto.RenewGoogleTokenDto;
import com.hanhome.youtube_comments.redis.service.RedisService;
import com.hanhome.youtube_comments.member.repository.MemberRepository;
import com.hanhome.youtube_comments.utils.AESUtil;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RenewGoogleTokenService {
    private final MemberRepository memberRepository;
    private final RedisService redisService;
    private final AESUtil aesUtil;

    private WebClient webClient = WebClient.create("https://oauth2.googleapis.com/token");

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;
    @Value("${data.youtube.access-token}")
    private String redisGoogleAtKey;

    public String renewAccessToken(UUID uuid) throws Exception {
        String refreshToken = memberRepository.findById(uuid).get().getGoogleRefreshToken();
        String decryptedToken = aesUtil.decrypt(refreshToken);
        RenewGoogleTokenDto.Request dto =
                RenewGoogleTokenDto.Request.builder()
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .grantType("refresh_token")
                        .refreshToken(decryptedToken)
                        .build();

        RenewGoogleTokenDto.Response response = webClient.post()
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(jsonNode -> {
                    RenewGoogleTokenDto.Response res = RenewGoogleTokenDto.Response.builder()
                            .accessToken(jsonNode.path("access_token").asText())
                            .expiresIn(jsonNode.path("expires_in").asInt())
                            .build();
                    return Mono.just(res);
                })
                .block();

        redisService.save(redisGoogleAtKey + ":" + uuid, response.getAccessToken(), response.getExpiresIn(), TimeUnit.SECONDS);

        return response.getAccessToken();
    }
}
