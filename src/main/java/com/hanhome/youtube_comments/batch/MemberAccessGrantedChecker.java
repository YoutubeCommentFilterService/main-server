package com.hanhome.youtube_comments.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MemberAccessGrantedChecker {
    private final JobLauncher jobLauncher;
    private final Job removeAccessNotGrantedMemberJob;

    public MemberAccessGrantedChecker(
            JobLauncher jobLauncher,
            @Qualifier("removeUnlinkedMemberJob") Job removeAccessNotGrantedMemberJob
    ) {
        this.jobLauncher = jobLauncher;
        this.removeAccessNotGrantedMemberJob = removeAccessNotGrantedMemberJob;
    }

    @Scheduled(cron = "0 30 1,13 * * *", zone = "Asia/Seoul")
    public void run() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(
                    removeAccessNotGrantedMemberJob,
                    jobParameters
            );
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
