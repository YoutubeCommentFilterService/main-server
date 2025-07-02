package com.hanhome.youtube_comments.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class YoutubeChannelInfoUpdater {
    private final JobLauncher jobLauncher;
    private final Job updateMemberProfileJob;

    public YoutubeChannelInfoUpdater(
            JobLauncher jobLauncher,
            @Qualifier("updateMemberProfileJob") Job updateMemberProfileJob
    ) {
        this.jobLauncher = jobLauncher;
        this.updateMemberProfileJob = updateMemberProfileJob;
    }

    @Scheduled(cron = "0 0 1 * * 1", zone = "Asia/Seoul")
    public void run() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(
                    updateMemberProfileJob,
                    jobParameters
            );
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
