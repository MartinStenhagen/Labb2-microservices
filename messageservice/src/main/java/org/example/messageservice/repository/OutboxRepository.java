package org.example.messageservice.repository;

import org.example.messageservice.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatus(OutboxEvent.OutboxStatus status);

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus status);

    List<OutboxEvent> findByStatusAndLastAttemptAtBeforeOrderByCreatedAtAsc(
            OutboxEvent.OutboxStatus status,
            LocalDateTime lastAttemptAt
    );
}
