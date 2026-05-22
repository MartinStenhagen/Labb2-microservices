package org.example.messageservice.repository;

import org.example.messageservice.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<ChatMessage, Long> {
    Optional<ChatMessage> findBySourceEventId(UUID sourceEventId);

    List<ChatMessage> findByRoomOrderByCreatedAtAsc(String room);
}
