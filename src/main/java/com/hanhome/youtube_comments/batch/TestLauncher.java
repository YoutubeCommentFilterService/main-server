package com.hanhome.youtube_comments.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

// 아래의 어노테이션이 없으면 실행되지 않는다.
//@Component
public class TestLauncher implements ApplicationRunner {
    private final JobLauncher jobLauncher;
    private final Job job;

    public TestLauncher(
        JobLauncher jobLauncher,
        @Qualifier("") Job job
    ) {
        this.jobLauncher = jobLauncher;
        this.job = job;
    }
    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("Test Runner Started!");
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        try {
            // JobLauncher를 사용하여 Job 실행
            jobLauncher.run(job, jobParameters);
            System.out.println("Spring Batch Job 'testing' executed successfully!");
        } catch (JobExecutionAlreadyRunningException e) {
            System.err.println("Job 'testing' is already running: " + e.getMessage());
        } catch (JobRestartException e) {
            System.err.println("Job 'testing' cannot be restarted: " + e.getMessage());
        } catch (JobInstanceAlreadyCompleteException e) {
            System.err.println("Job 'testing' has already completed for these parameters: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error executing Job 'testing': " + e.getMessage());
            e.printStackTrace();
        }
    }
}
