package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.entity.OutboxEvent;
import com.dhruv.microloan_platform.entity.OutboxEventStatus;
import com.dhruv.microloan_platform.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest against the real H2 DB, with OutboxPublisher constructed manually over the
 * autowired repository (it only needs that one dependency - no need for a full
 * @SpringBootTest context just to exercise this).
 *
 * This is the proof for "none is double-published": the guarantee isn't code remembering
 * what it already sent, it's that the query itself (status = PENDING) structurally excludes
 * an already-published row from ever being selected again, however many times this runs.
 */
@DataJpaTest
class OutboxPublisherTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private OutboxPublisher outboxPublisher;

    private OutboxEvent newPendingEvent(String eventType) {
        return OutboxEvent.builder()
                .aggregateType("LOAN")
                .aggregateId(1L)
                .eventType(eventType)
                .payload("{}")
                .build();
    }

    @Test
    void secondRunPublishesNothingAfterFirstRunPublishesEverythingPending() {
        outboxPublisher = new OutboxPublisher(outboxEventRepository);

        outboxEventRepository.save(newPendingEvent("LOAN_APPROVED"));
        outboxEventRepository.save(newPendingEvent("REPAYMENT_RECEIVED"));
        outboxEventRepository.save(newPendingEvent("LOAN_OVERDUE"));

        int firstRunCount = outboxPublisher.publishPending();
        assertThat(firstRunCount).isEqualTo(3);
        assertThat(outboxEventRepository.findByStatus(OutboxEventStatus.PENDING, PageRequest.of(0, 10)).getTotalElements())
                .isZero();
        assertThat(outboxEventRepository.findByStatus(OutboxEventStatus.PUBLISHED, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(3);
        assertThat(outboxEventRepository.findAll()).allSatisfy(event -> assertThat(event.getPublishedAt()).isNotNull());

        int secondRunCount = outboxPublisher.publishPending();
        assertThat(secondRunCount).isZero();
        assertThat(outboxEventRepository.findByStatus(OutboxEventStatus.PUBLISHED, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(3);
    }

    @Test
    void doesNotTouchAlreadyPublishedEvents() {
        outboxPublisher = new OutboxPublisher(outboxEventRepository);
        OutboxEvent alreadyPublished = outboxEventRepository.saveAndFlush(OutboxEvent.builder()
                .aggregateType("LOAN").aggregateId(1L).eventType("LOAN_APPROVED").payload("{}")
                .status(OutboxEventStatus.PUBLISHED)
                .build());

        int published = outboxPublisher.publishPending();

        assertThat(published).isZero();
        assertThat(outboxEventRepository.findById(alreadyPublished.getId()).orElseThrow().getPublishedAt()).isNull();
    }
}
