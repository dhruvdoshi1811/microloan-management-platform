package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.entity.OutboxEvent;
import com.dhruv.microloan_platform.entity.OutboxEventStatus;
import com.dhruv.microloan_platform.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Polls PENDING outbox rows and "publishes" them - there's no real message broker/webhook
 * target in this project (same demo-boundary honesty as KYC's mocked OTP in Phase A), so
 * publishing here just means logging what would have been sent, then marking the row
 * PUBLISHED. The actual guarantee this class provides isn't "nothing gets sent twice to some
 * external system" (there is no such system) - it's that the query itself, filtering on
 * status = PENDING, makes it structurally impossible for an already-published row to be
 * selected again by any future call, however many times this runs.
 */
@Service
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int PAGE_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    /** Processes one page of PENDING events per call; a backlog larger than that drains over subsequent calls. */
    @Transactional
    public int publishPending() {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Page<OutboxEvent> page = outboxEventRepository.findByStatus(OutboxEventStatus.PENDING, pageable);

        Instant now = Instant.now();
        for (OutboxEvent event : page) {
            log.info("Publishing outbox event id={} type={} aggregate={}#{}",
                    event.getId(), event.getEventType(), event.getAggregateType(), event.getAggregateId());
            event.setStatus(OutboxEventStatus.PUBLISHED);
            event.setPublishedAt(now);
        }
        outboxEventRepository.saveAll(page);

        return page.getNumberOfElements();
    }
}
