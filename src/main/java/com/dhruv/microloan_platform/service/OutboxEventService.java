package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.dto.outbox.OutboxEventResponse;
import com.dhruv.microloan_platform.entity.OutboxEvent;
import com.dhruv.microloan_platform.entity.OutboxEventStatus;
import com.dhruv.microloan_platform.repository.OutboxEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;

    public OutboxEventService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    public Page<OutboxEventResponse> list(OutboxEventStatus status, Pageable pageable) {
        Page<OutboxEvent> page = status != null
                ? outboxEventRepository.findByStatus(status, pageable)
                : outboxEventRepository.findAll(pageable);
        return page.map(OutboxEventResponse::from);
    }
}
