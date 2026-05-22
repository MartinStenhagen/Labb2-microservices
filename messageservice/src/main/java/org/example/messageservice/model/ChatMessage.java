package org.example.messageservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long senderUserId;

    private String senderUsername;

    private String room = "general";

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(unique = true)
    private UUID sourceEventId;

    private LocalDateTime createdAt = LocalDateTime.now();

    public ChatMessage() {
    }

    public Long getId() {
        return id;
    }

    public Long getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(Long senderUserId) {
        this.senderUserId = senderUserId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(UUID sourceEventId) {
        this.sourceEventId = sourceEventId;
    }
}
