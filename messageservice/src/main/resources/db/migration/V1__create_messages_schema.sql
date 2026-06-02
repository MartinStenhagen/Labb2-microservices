CREATE TABLE IF NOT EXISTS messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sender_user_id BIGINT,
    sender_username VARCHAR(255),
    content TEXT,
    created_at DATETIME(6),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS outbox_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BINARY(16),
    aggregate_type VARCHAR(255),
    aggregate_id BIGINT,
    type VARCHAR(255),
    payload TEXT,
    created_at DATETIME(6),
    status ENUM('PENDING', 'PROCESSED', 'FAILED'),
    PRIMARY KEY (id)
);
