package com.hanhome.youtube_comments.batch;

import com.hanhome.youtube_comments.google.service.GeminiService;
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
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

// 아래의 어노테이션이 없으면 실행되지 않는다.
//@Component
public class TestLauncher implements ApplicationRunner {
    private final JobLauncher jobLauncher;
    private final Job job;
    private final GeminiService geminiService;

    public TestLauncher(
        JobLauncher jobLauncher,
        @Qualifier("updateHotVideoAndSaveJob") Job job,
        GeminiService geminiService
    ) {
        this.jobLauncher = jobLauncher;
        this.job = job;
        this.geminiService = geminiService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("Test Runner Started!");

//        fetchGeminiSummarize();
//        runTestJob();
    }

    private void runTestJob() {
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

    private void fetchGeminiSummarize() {
        String[] urls = {
                "9oBrILNOfd4",
                "1KzZnr1Zl0c",
                "TsLr7BdQWpU",
                "AiRZJOQSLaA",
                "8yliKIyKNSE",
        };

        Map<String, String> geminiOutputs = geminiService.generateSummarizationVideoCaptions(List.of(urls));

        for (Map.Entry<String, String> output : geminiOutputs.entrySet()) {
            System.out.println(output.getKey() + "\n" + output.getValue() + "\n\n");
        }
    }
}
