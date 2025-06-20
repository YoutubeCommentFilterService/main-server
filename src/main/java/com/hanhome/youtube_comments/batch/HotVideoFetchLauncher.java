package com.hanhome.youtube_comments.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class HotVideoFetchLauncher implements ApplicationRunner {
    private final JobLauncher jobLauncher;
    private final Job job;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${data.youtube.hot-video}")
    private String REDIS_HOTVIDEO_KEY;

    public HotVideoFetchLauncher(
            JobLauncher jobLauncher,
            @Qualifier("updateHotVideoAndSaveJob") Job job,
            @Qualifier("redisTemplate") RedisTemplate<String, Object> redisTemplate
    ) {
        this.jobLauncher = jobLauncher;
        this.job = job;
        this.redisTemplate = redisTemplate;
    }
    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (redisTemplate.hasKey(REDIS_HOTVIDEO_KEY)) return;
        System.out.println("HotVideo Runner Started!");
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        try {
            // JobLauncher를 사용하여 Job 실행
            jobLauncher.run(job, jobParameters);
            System.out.println("Spring Batch Job 'hotVideo' executed successfully!");
        } catch (JobExecutionAlreadyRunningException e) {
            System.err.println("Job 'hotVideo' is already running: " + e.getMessage());
        } catch (JobRestartException e) {
            System.err.println("Job 'hotVideo' cannot be restarted: " + e.getMessage());
        } catch (JobInstanceAlreadyCompleteException e) {
            System.err.println("Job 'hotVideo' has already completed for these parameters: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error executing Job 'hotVideo': " + e.getMessage());
            e.printStackTrace();
        }
    }
}
