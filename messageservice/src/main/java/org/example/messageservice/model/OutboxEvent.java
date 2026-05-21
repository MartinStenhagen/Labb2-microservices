package org.example.messageservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private UUID eventId;
    private String aggregateType;
    private Long aggregateId;
    private String type;
    
    @Column(columnDefinition = "TEXT")
    private String payload;
    
    private LocalDateTime createdAt;
    private int publishAttempts;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime processedAt;
    private LocalDateTime failedAt;

    @Column(columnDefinition = "TEXT")
    private String lastError;
    
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    public enum OutboxStatus {
        PENDING, PUBLISHING, PROCESSED, FAILED
    }

    public OutboxEvent() {}

    public OutboxEvent(UUID eventId, String aggregateType, Long aggregateId, String type, String payload) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.type = type;
        this.payload = payload;
        this.createdAt = LocalDateTime.now();
        this.publishAttempts = 0;
        this.status = OutboxStatus.PENDING;
    }

    public void markPublishingAttempt() {
        this.publishAttempts++;
        this.lastAttemptAt = LocalDateTime.now();
        this.status = OutboxStatus.PUBLISHING;
    }

    public void markPending(String error) {
        this.status = OutboxStatus.PENDING;
        this.lastError = trimError(error);
    }

    public void markProcessed() {
        this.status = OutboxStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.status = OutboxStatus.FAILED;
        this.failedAt = LocalDateTime.now();
        this.lastError = trimError(error);
    }

    public boolean hasReachedMaxAttempts(int maxAttempts) {
        return publishAttempts >= maxAttempts;
    }

    private String trimError(String error) {
        if (error == null) {
            return null;
        }

        return error.length() <= 1000 ? error : error.substring(0, 1000);
    }

    public Long getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getAggregateType() { return aggregateType; }
    public Long getAggregateId() { return aggregateId; }
    public String getType() { return type; }
    public String getPayload() { return payload; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public int getPublishAttempts() { return publishAttempts; }
    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public LocalDateTime getFailedAt() { return failedAt; }
    public String getLastError() { return lastError; }
    public OutboxStatus getStatus() { return status; }
    public void setStatus(OutboxStatus status) { this.status = status; }
}
