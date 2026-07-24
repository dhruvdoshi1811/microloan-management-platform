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

@Service
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int PAGE_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

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
