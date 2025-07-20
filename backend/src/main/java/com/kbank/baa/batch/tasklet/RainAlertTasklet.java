package com.kbank.baa.batch.tasklet;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class RainAlertTasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution,
                                ChunkContext chunkContext) {
        // TODO: 기상청 API 호출 → 강우량 체크 → 텔레그램 메시지 발송
        System.out.println("##### RAIN ALERT BATCH #####");
        return RepeatStatus.FINISHED;
    }
}
