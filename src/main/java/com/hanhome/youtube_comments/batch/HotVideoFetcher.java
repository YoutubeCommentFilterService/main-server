package com.hanhome.youtube_comments.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HotVideoFetcher {
    private final JobLauncher jobLauncher;
    private final Job job;

    public HotVideoFetcher(
            JobLauncher jobLauncher,
            @Qualifier("updateHotVideoAndSaveJob") Job job
    ) {
        this.jobLauncher = jobLauncher;
        this.job = job;
    }

    @Scheduled(cron = "0 0 8,20 * * *", zone = "Asia/Seoul")
    public void run() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(
                    job,
                    jobParameters
            );
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
