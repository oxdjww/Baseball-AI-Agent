package com.kbank.baa.batch;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class GameAnalysisTasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution,
                                ChunkContext chunkContext) {
        // TODO: KBO API 호출 → 회원 조회 → 통계 계산 → 텔레그램 메시지 발송
        System.out.println("##### BATCH TEST #####");
        return RepeatStatus.FINISHED;
    }
}
