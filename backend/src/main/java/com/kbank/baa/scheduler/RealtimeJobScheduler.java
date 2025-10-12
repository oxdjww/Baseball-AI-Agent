package com.kbank.baa.scheduler;

import com.kbank.baa.member.MemberRepository;
import com.kbank.baa.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RealtimeJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job realTimeAlertJob;
    private final MemberRepository memberRepository;
    private final TelegramService telegramService;

    @Value("${telegram.admin-id}")
    private String telegramAdminId;


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

    /**
     * 매일 00:00 KST 실행
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void purgeMembersWithoutTelegram() {
        final Instant before = Instant.now().minus(Duration.ofHours(24));

        try {
            int affected = memberRepository.deleteOldPending(before);
            log.warn("[purgeMembersWithoutTelegram] 하드 삭제 rows={}", affected);

            if (affected > 0) {
                log.info("[purgeMembersWithoutTelegram] 관리자({}) 알림톡 전송", telegramAdminId);

                ZonedDateTime nowKst = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
                String msg = String.format(
                        "<b>[배치] 텔레그램 미연동 회원 정리</b>\n" +
                                "삭제건수: <b>%,d</b>건\n" +
                                "기준: 24h 경과 & telegramId=NULL\n" +
                                "실행시각(KST): %s",
                        affected,
                        nowKst
                );
                telegramService.sendPlainMessage(telegramAdminId, msg);
            }
        } catch (Exception e) {
            log.error("[purgeMembersWithoutTelegram] 실패: {}", e.getMessage(), e);
        }
    }
}
