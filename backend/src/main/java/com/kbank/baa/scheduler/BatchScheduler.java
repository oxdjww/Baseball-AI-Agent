package com.kbank.baa.scheduler;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job dailyGameAnalysisJob;
    private final Job rainAlertJob;
    private final Job realTimeAlertJob;

    public BatchScheduler(JobLauncher jobLauncher,
                          Job dailyGameAnalysisJob,
                          Job rainAlertJob,
                          Job realTimeAlertJob) {
        this.jobLauncher = jobLauncher;
        this.dailyGameAnalysisJob = dailyGameAnalysisJob;
        this.rainAlertJob = rainAlertJob;
        this.realTimeAlertJob = realTimeAlertJob;
    }

    // 매일 자정에 배치 테스트(job 이름만 달라서 재사용 가능)
    @Scheduled(cron = "0 45 23 * * *")
    public void runDailyGameAnalysis() throws Exception {
        jobLauncher.run(dailyGameAnalysisJob,
                new JobParametersBuilder()
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters());
    }

    // 경기 3시간 전 우천 알림
    @Scheduled(cron = "0 0  * * * *")  // TODO: 동적 cron 으로 변경 가능
    public void runRainAlert3h() throws Exception {
        jobLauncher.run(rainAlertJob,
                new JobParametersBuilder()
                        .addString("type", "3h")
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters());
    }

    // 경기 1시간 전 우천 알림
    @Scheduled(cron = "0 0 * * * *")   // TODO: 동적 cron 으로 변경 가능
    public void runRainAlert1h() throws Exception {
        jobLauncher.run(rainAlertJob,
                new JobParametersBuilder()
                        .addString("type", "1h")
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters());
    }

    // 실시간 모니터링 (5분마다)
//    @Scheduled(fixedDelay = 300_000) // 5분
    @Scheduled(fixedDelay = 3_600_000) // 60분
    public void runRealTimeAlert() throws Exception {
        jobLauncher.run(realTimeAlertJob,
                new JobParametersBuilder()
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters());
    }
}
