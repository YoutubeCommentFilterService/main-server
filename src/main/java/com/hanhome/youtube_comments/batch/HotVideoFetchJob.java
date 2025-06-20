package com.hanhome.youtube_comments.batch;

import com.hanhome.youtube_comments.google.dto.GetHotVideosDto;
import com.hanhome.youtube_comments.google.service.YoutubeDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class HotVideoFetchJob {
    private final YoutubeDataService youtubeDataService;
    private final PlatformTransactionManager transactionManager;
    private final JobRepository jobRepository;

    @Value("${data.youtube.hot-video}")
    private String REDIS_HOTVIDEO_KEY;

    @Bean
    public Tasklet updateHotVideoAndSave(
            @Qualifier("redisTemplate") RedisTemplate<String, Object> redisTemplate
    ) throws Exception {
        return (StepContribution contribution, ChunkContext chunkContext) -> {
            try {
                // RetryTemplate 사용
                RetryTemplate retryTemplate = new RetryTemplate();
                retryTemplate.execute(context -> {
                    GetHotVideosDto.Response response = youtubeDataService.getHotVideos();
                    redisTemplate.opsForValue()
                            .set(REDIS_HOTVIDEO_KEY, response);
                    return null;
                });
            } catch (Exception e) {
                // 로그 남기고 예외 던져서 Spring Batch가 실패로 인식하게
                log.error("Hot video 업데이트 실패", e);
                throw e;
            }
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step updateHotVideoStep(
            @Qualifier("updateHotVideoAndSave") Tasklet tasklet
    ) {
        return new StepBuilder("updateHotVideo", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Job updateHotVideoAndSaveJob(
            @Qualifier("updateHotVideoStep") Step step
    ) {
        return new JobBuilder("updateHotVideoJob", jobRepository)
                .start(step)
                .build();
    }
}
