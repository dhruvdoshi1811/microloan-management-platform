package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.entity.OutboxEvent;
import com.dhruv.microloan_platform.entity.OutboxEventStatus;
import com.dhruv.microloan_platform.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventWriterTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private record SamplePayload(String message, int count) {
    }

    @Test
    void buildsAndSavesAPendingEventWithSerializedPayload() {
        OutboxEventWriter writer = new OutboxEventWriter(outboxEventRepository, objectMapper);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        writer.write("LOAN", 42L, "LOAN_APPROVED", new SamplePayload("hello", 3));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent saved = captor.getValue();

        assertThat(saved.getAggregateType()).isEqualTo("LOAN");
        assertThat(saved.getAggregateId()).isEqualTo(42L);
        assertThat(saved.getEventType()).isEqualTo("LOAN_APPROVED");
        assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(saved.getPayload()).contains("\"message\":\"hello\"").contains("\"count\":3");
    }
}
