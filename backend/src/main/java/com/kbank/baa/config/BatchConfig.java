package com.kbank.baa.config;

import com.kbank.baa.batch.tasklet.GameAnalysisTasklet;
import com.kbank.baa.batch.tasklet.RainAlertTasklet;
import com.kbank.baa.batch.tasklet.RealTimeAlertTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;


@EnableBatchProcessing
@Configuration
public class BatchConfig {

    @Bean
    public Step gameAnalysisStep(JobRepository jobRepository, PlatformTransactionManager tm) {
        return new StepBuilder("gameAnalysisStep", jobRepository)
                .tasklet(new GameAnalysisTasklet(), tm)
                .build();
    }

    @Bean
    public Job dailyGameAnalysisJob(JobRepository jobRepository, Step gameAnalysisStep) {
        return new JobBuilder("dailyGameAnalysisJob", jobRepository)
                .start(gameAnalysisStep)
                .build();
    }

    @Bean
    public Step rainAlertStep(JobRepository jobRepository, PlatformTransactionManager tm) {
        return new StepBuilder("rainAlertStep", jobRepository)
                .tasklet(new RainAlertTasklet(), tm)
                .build();
    }

    @Bean
    public Job rainAlertJob(JobRepository jobRepository, Step rainAlertStep) {
        return new JobBuilder("rainAlertJob", jobRepository)
                .start(rainAlertStep)
                .build();
    }

    @Bean
    public Step realTimeAlertStep(
            JobRepository jobRepository,
            PlatformTransactionManager tm,
            RealTimeAlertTasklet realTimeAlertTasklet
    ) {
        return new StepBuilder("realTimeAlertStep", jobRepository)
                .tasklet(realTimeAlertTasklet, tm)
                .build();
    }

    @Bean
    public Job realTimeAlertJob(JobRepository jobRepository, Step realTimeAlertStep) {
        return new JobBuilder("realTimeAlertJob", jobRepository)
                .start(realTimeAlertStep)
                .build();
    }
}
