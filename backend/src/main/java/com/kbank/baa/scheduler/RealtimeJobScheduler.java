package com.kbank.baa.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RealtimeJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job realTimeAlertJob;

    /**
     * 야구 경기가 실제로 일어날 수 있는 시간에만 운영 (화-일 3분마다, 13:00–22:59 사이)
     */
    // @Scheduled(cron = "0 0/3 13-22 * * TUE-SUN", zone = "Asia/Seoul")
    // 포스트시즌 시간표 적용
    @Scheduled(cron = "0 0/3 14-22 * * *", zone = "Asia/Seoul")
    public void runRealTimeAlert() throws Exception {
        log.info("[RealtimeJobScheduler][runRealTimeAlert] 실행 시도");

        jobLauncher.run(realTimeAlertJob,
                new JobParametersBuilder()
                        .addLong("time", System.currentTimeMillis())
                        .addString("run.id", UUID.randomUUID().toString())
                        .toJobParameters());
        log.info("[RealtimeJobScheduler][runRealTimeAlert] 실행 요청 완료");
    }
}
