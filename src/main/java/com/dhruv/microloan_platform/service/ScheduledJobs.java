package com.dhruv.microloan_platform.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledJobs {

    private final OutboxPublisher outboxPublisher;
    private final OverdueService overdueService;

    public ScheduledJobs(OutboxPublisher outboxPublisher, OverdueService overdueService) {
        this.outboxPublisher = outboxPublisher;
        this.overdueService = overdueService;
    }

    @Scheduled(fixedDelay = 10_000)
    public void publishOutboxEvents() {
        outboxPublisher.publishPending();
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void runDailyOverdueCheck() {
        overdueService.runOverdueCheck();
    }
}
