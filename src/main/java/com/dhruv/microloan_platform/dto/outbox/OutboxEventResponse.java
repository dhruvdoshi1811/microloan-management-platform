package com.dhruv.microloan_platform.dto.outbox;

import com.dhruv.microloan_platform.entity.OutboxEvent;
import com.dhruv.microloan_platform.entity.OutboxEventStatus;

import java.time.Instant;

public record OutboxEventResponse(
        Long id,
        String aggregateType,
        Long aggregateId,
        String eventType,
        String payload,
        OutboxEventStatus status,
        Instant createdAt,
        Instant publishedAt
) {

    public static OutboxEventResponse from(OutboxEvent event) {
        return new OutboxEventResponse(
                event.getId(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getEventType(),
                event.getPayload(),
                event.getStatus(),
                event.getCreatedAt(),
                event.getPublishedAt());
    }
}
