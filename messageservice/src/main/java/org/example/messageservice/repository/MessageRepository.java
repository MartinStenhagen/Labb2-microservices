package org.example.messageservice.repository;

import org.example.messageservice.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<ChatMessage, Long> {}
