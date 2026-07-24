package com.dhruv.microloan_platform.repository;

import com.dhruv.microloan_platform.entity.OutboxEvent;
import com.dhruv.microloan_platform.entity.OutboxEventStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OutboxEventRepositoryTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private OutboxEvent newEvent(OutboxEventStatus status) {
        return OutboxEvent.builder()
                .aggregateType("LOAN")
                .aggregateId(1L)
                .eventType("LOAN_APPROVED")
                .payload("{\"loanId\":1}")
                .status(status)
                .build();
    }

    @Test
    void savesWithPendingDefaultAndNullPublishedAt() {
        OutboxEvent saved = outboxEventRepository.save(OutboxEvent.builder()
                .aggregateType("LOAN").aggregateId(1L).eventType("LOAN_APPROVED").payload("{}")
                .build());

        assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(saved.getPublishedAt()).isNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findsByStatusWithPaging() {
        outboxEventRepository.save(newEvent(OutboxEventStatus.PENDING));
        outboxEventRepository.save(newEvent(OutboxEventStatus.PENDING));
        outboxEventRepository.save(newEvent(OutboxEventStatus.PUBLISHED));

        var pendingPage = outboxEventRepository.findByStatus(OutboxEventStatus.PENDING, PageRequest.of(0, 10));
        var publishedPage = outboxEventRepository.findByStatus(OutboxEventStatus.PUBLISHED, PageRequest.of(0, 10));

        assertThat(pendingPage.getTotalElements()).isEqualTo(2);
        assertThat(publishedPage.getTotalElements()).isEqualTo(1);
    }
}
