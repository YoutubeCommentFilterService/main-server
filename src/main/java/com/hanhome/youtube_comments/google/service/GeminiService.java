package com.hanhome.youtube_comments.google.service;

import com.hanhome.youtube_comments.google.object.gemini.SummarizeYoutubeSubtitleRequest;
import com.hanhome.youtube_comments.google.object.gemini.SummarizeYoutubeSubtitleResponse;
import com.hanhome.youtube_comments.google.object.gemini.gemini_body.GenerationConfig;
import com.hanhome.youtube_comments.google.object.gemini.gemini_body.Part;
import com.hanhome.youtube_comments.google.object.gemini.gemini_body.ThinkingConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class GeminiService {
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .build();
    private static final String modelType = "gemini-2.5-flash-lite-preview-06-17";
    private static final int SUMMARIZE_BATCH_SIZE = 15;

    @Value("${data.gemini.key}")
    private String GEMINI_KEY;
    @Value("${data.ytdlp.script-path}")
    private String YTDLP_SCRIPT_PATH;

    public Map<String, String> generateSummarizationVideoCaptions(List<String> videoIds) {
        // Gemini free tier 한도로 인해 비동기로 작업 X
        // 추후 돈 많이 벌고 광고로 충당되면 비동기로 작업
        Map<String, String> subtitleFeatures = fetchSubtitleFeatures(videoIds);
        return fetchGeminiSummarizations(subtitleFeatures);
    }

    private Map<String, String> fetchSubtitleFeatures(List<String> urls) {
        ExecutorService ytdlpExecutor = Executors.newFixedThreadPool(10);

        Map<String, CompletableFuture<String>> futures = urls.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        url -> CompletableFuture.supplyAsync(() -> fetchSubtitle(url), ytdlpExecutor)
                ));
        CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).join();
        ytdlpExecutor.shutdown();

        return futures.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().join()))
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String fetchSubtitle(String videoIdUrl) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("python", YTDLP_SCRIPT_PATH, videoIdUrl);

        try {
            Process process = processBuilder.start();

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
                    );

            StringBuilder stringBuilder = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            return stringBuilder.toString();
        } catch (IOException e) {
            return "";
        }
    }

    private Map<String, String> fetchGeminiSummarizations(Map<String, String> ytdlpOutputs) {
        List<Map.Entry<String, String>> entries = new ArrayList<>(ytdlpOutputs.entrySet());
        System.out.println(entries.size());

        return IntStream.range(0, (entries.size() + SUMMARIZE_BATCH_SIZE - 1) / SUMMARIZE_BATCH_SIZE)
                .mapToObj(idx -> {
                    int from = idx * SUMMARIZE_BATCH_SIZE;
                    int to = Math.min(from + SUMMARIZE_BATCH_SIZE, entries.size());

                    List<Map.Entry<String, String>> batch = entries.subList(from, to);

                    Map<String, CompletableFuture<String>> futures = batch.stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    item -> fetchGeminiSummarization(item.getValue())
                            ));
                    CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).join();

                    Map<String, String> result = futures.entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> entry.getValue().join()
                            ));

                    if (to < entries.size()) {
                        try {
                            Thread.sleep(60_000);
                        } catch (InterruptedException e) {
                            // Thread interrupt 로직 없음. 빈 로직으로
                        }
                    }

                    return result;
                })
                .flatMap(mapStream -> mapStream.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    private CompletableFuture<String> fetchGeminiSummarization(String text) {
            SummarizeYoutubeSubtitleRequest request = new SummarizeYoutubeSubtitleRequest();
            request.getSystemInstruction().putPart(new Part("너는 주어진 문장을 요약하는 역할이야. 요약 내용은 개조식으로 작성해. 요약 문장의 개수는 5개, 각 문장의 시작 문자는 '-' 야. 요약 내용만 반환해."));
            request.putContentPart(new Part(text));
            request.setGenerationConfig(new GenerationConfig(new ThinkingConfig(0)));

            return webClient.post()
                    .uri("/v1beta/models/" + modelType + ":generateContent?key=" + GEMINI_KEY)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(SummarizeYoutubeSubtitleResponse.class)
                    .map(res -> res.getCandidates().get(0).getContent().getParts().get(0).getText())
                    .toFuture();
    }

}
