package com.hanhome.youtube_comments.member.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
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

    @Scheduled(cron = "0 0 */12 * * *")
    public void run() {
        try {
            String dynamicJobName = "removeUnwarrantedMember-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("mm-ss-SSS"));

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("jobNameSuffix", dynamicJobName)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(removeAccessNotGrantedMemberJob, jobParameters);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
