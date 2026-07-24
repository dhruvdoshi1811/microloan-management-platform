package com.dhruv.microloan_platform.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Pure scheduling concern, deliberately separate from OutboxPublisher/OverdueService: this
 * class only knows WHEN to trigger, the services only know WHAT to do when triggered -
 * mirrors the controller/service split used everywhere else (HTTP is one trigger,
 * @Scheduled is another, both call the same underlying methods).
 *
 * Gated behind app.scheduling.enabled (default true, set false in test properties) - without
 * this, a live 10-second poller running inside every @SpringBootTest-cached context would be
 * a real source of test flakiness, not a hypothetical one.
 */
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
