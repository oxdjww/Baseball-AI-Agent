package com.kbank.baa.batch;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class RealTimeAlertTasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution,
                                ChunkContext chunkContext) {
        // TODO: 네이버 스포츠 API 폴링 → 이벤트 감지 → 텔레그램 메시지 발송
        System.out.println("##### REAL-TIME ALERT BATCH #####");
        return RepeatStatus.FINISHED;
    }
}