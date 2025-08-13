package com.kbank.baa.test;

import com.kbank.baa.batch.tasklet.RealTimeAlertTasklet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RealtimeTestController {

    private final RealTimeAlertTasklet tasklet;

    @GetMapping("/test/realAlert")
    public String runRealTimeAlertTasklet() {
        try {
            StepExecution dummyStepExecution = new StepExecution("dummyStep", null);
            StepContribution dummyContribution = new StepContribution(dummyStepExecution);

            ChunkContext dummyChunkContext = new ChunkContext(new StepContext(dummyStepExecution));

            RepeatStatus status = tasklet.execute(dummyContribution, dummyChunkContext);
            return "[RealtimeTestController][runRealTimeAlertTasklet] ✅ Tasklet 실행 완료: " + status;
        } catch (Exception e) {
            log.error("[RealtimeTestController][runRealTimeAlertTasklet] ❌ Tasklet 실행 실패", e);
            return "❌ 실행 실패: " + e.getMessage();
        }
    }
}
