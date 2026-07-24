package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.entity.OutboxEvent;
import com.dhruv.microloan_platform.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Shared cross-cutting helper injected into every service that needs to record a
 * notification-worthy state change - not a domain service with its own business rules, the
 * same category as injecting ObjectMapper directly already is. Callers are responsible for
 * calling this from WITHIN their own @Transactional method, so the OutboxEvent row commits
 * (or rolls back) atomically with whatever state change it's describing - that atomicity is
 * the entire point of the transactional outbox pattern.
 */
@Service
public class OutboxEventWriter {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventWriter(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void write(String aggregateType, Long aggregateId, String eventType, Object payload) {
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(objectMapper.writeValueAsString(payload))
                .build();

        outboxEventRepository.save(event);
    }
}
