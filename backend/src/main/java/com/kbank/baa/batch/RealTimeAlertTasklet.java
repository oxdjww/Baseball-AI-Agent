package com.kbank.baa.batch;

import com.kbank.baa.sports.GameMessageFormatter;
import com.kbank.baa.sports.SportsApiClient;
import com.kbank.baa.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RealTimeAlertTasklet implements Tasklet {

    private final SportsApiClient apiClient;
    private final GameMessageFormatter formatter;
    private final TelegramService telegram;

    @Override
    public RepeatStatus execute(StepContribution contribution,
                                ChunkContext chunkContext) {
        // 1) gameId 추출 (예시는 리터럴, 실전에선 파라미터나 날짜 기반으로)
        String gameId = "20240827SSWO02024";

        // 2) API 호출 + 정보 획득
        var info = apiClient.fetchGameInfo(gameId);

        // 3) 메시지 생성
        String message = formatter.format(info);

        // 4) 텔레그램 전송
        telegram.sendMessage(message);

        return RepeatStatus.FINISHED;
    }
}
