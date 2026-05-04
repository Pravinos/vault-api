package com.vfa.vault.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.vfa.vault.service.WeeklySummaryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeeklySummaryScheduler {

    private final WeeklySummaryService weeklySummaryService;

    @Scheduled(cron = "0 0 8 * * MON")
    public void generateWeeklySummary() {
        weeklySummaryService.generateScheduled();
        log.info("Weekly summary scheduler run finished");
    }
}
