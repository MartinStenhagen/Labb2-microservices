package org.example.event;

import java.time.LocalDateTime;
import java.util.UUID;

public class MessagePublishedEvent {
    private UUID eventId;
    private Long messageId;
    private Long senderUserId;
    private String senderUsername;
    private String room;
    private String content;
    private LocalDateTime publishedAt;

    public MessagePublishedEvent() {
    }

    public MessagePublishedEvent(
            UUID eventId,
            Long messageId,
            Long senderUserId,
            String senderUsername,
            String room,
            String content,
            LocalDateTime publishedAt
    ) {
        this.eventId = eventId;
        this.messageId = messageId;
        this.senderUserId = senderUserId;
        this.senderUsername = senderUsername;
        this.room = room;
        this.content = content;
        this.publishedAt = publishedAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
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

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public UUID eventId() {
        return eventId;
    }

    public Long messageId() {
        return messageId;
    }

    public Long senderUserId() {
        return senderUserId;
    }

    public String senderUsername() {
        return senderUsername;
    }

    public String room() {
        return room;
    }

    public String content() {
        return content;
    }

    public LocalDateTime publishedAt() {
        return publishedAt;
    }
}
