package com.dhruv.microloan_platform.entity;

/** Lifecycle state of an {@link OutboxEvent}. PENDING until OutboxPublisher picks it up. */
public enum OutboxEventStatus {
    PENDING,
    PUBLISHED
}
