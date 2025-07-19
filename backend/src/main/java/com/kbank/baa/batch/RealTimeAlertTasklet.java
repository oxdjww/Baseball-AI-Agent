package com.kbank.baa.batch;

import com.kbank.baa.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@RequiredArgsConstructor
public class RealTimeAlertTasklet implements Tasklet {

    private final TelegramService telegram;

    @Override
    public RepeatStatus execute(StepContribution contribution,
                                ChunkContext chunkContext) {
        // TODO: 네이버 스포츠 API 폴링 → 이벤트 감지 → 텔레그램 메시지 발송
        System.out.println("##### REAL-TIME ALERT BATCH #####");
        telegram.sendMessage("📣 실시간 알림 테스트 메시지!");
        return RepeatStatus.FINISHED;
    }
}