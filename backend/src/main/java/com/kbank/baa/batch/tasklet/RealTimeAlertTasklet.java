// RealTimeAlertTasklet.java
package com.kbank.baa.batch.tasklet;

import com.kbank.baa.batch.service.GameAlertService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Slf4j
@RequiredArgsConstructor
public class RealTimeAlertTasklet implements Tasklet {
    private final GameAlertService alertService;

    @Override
    public RepeatStatus execute(@NonNull StepContribution contribution, @NonNull ChunkContext chunkContext) {
        log.info("[RealTimeAlertTasklet][execute] RealTimeAlertTasklet.execute 시작 ##########");
        alertService.processAlertsFor(LocalDate.now());
        log.info("[RealTimeAlertTasklet][execute] RealTimeAlertTasklet.execute 완료 ##########");
        return RepeatStatus.FINISHED;
    }
}