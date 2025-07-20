package com.kbank.baa.telegram;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class TelegramJobListener implements JobExecutionListener {
    private final TelegramService bot;

    public TelegramJobListener(TelegramService bot) {
        this.bot = bot;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        bot.sendMessage("배치 시작: " + jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String status = jobExecution.getStatus().name();
        bot.sendMessage("배치 " + status + ": " + jobExecution.getJobInstance().getJobName());
    }
}

