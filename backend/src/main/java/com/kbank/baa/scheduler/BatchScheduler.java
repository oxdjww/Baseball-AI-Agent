package com.kbank.baa.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job dailyGameAnalysisJob;
    private final Job realTimeAlertJob;

    // 매일 자정에 배치 테스트(job 이름만 달라서 재사용 가능)
    @Scheduled(cron = "0 45 23 * * *")
    public void runDailyGameAnalysis() throws Exception {
        jobLauncher.run(dailyGameAnalysisJob,
                new JobParametersBuilder()
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters());
    }

    // TODO: AI 분석 Job 추가 필요

//    @Scheduled(fixedDelay = 300_000) // 5분, 테스트용

    /**
     * 야구 경기가 실제로 일어날 수 있는 시간에만 운영 (3분마다, 13:00–21:59 사이)
     */
    @Scheduled(cron = "0 0/3 13-21 * * *", zone = "Asia/Seoul")
    public void runRealTimeAlert() throws Exception {
        jobLauncher.run(realTimeAlertJob,
                new JobParametersBuilder()
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters());
    }
}
