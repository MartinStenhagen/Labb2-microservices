CREATE TABLE IF NOT EXISTS messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sender_user_id BIGINT,
    sender_username VARCHAR(255),
    content TEXT,
    source_event_id BINARY(16),
    created_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_messages_source_event_id UNIQUE (source_event_id)
);

CREATE TABLE IF NOT EXISTS outbox_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BINARY(16),
    aggregate_type VARCHAR(255),
    aggregate_id BIGINT,
    type VARCHAR(255),
    payload TEXT,
    created_at DATETIME(6),
    publish_attempts INT NOT NULL DEFAULT 0,
    last_attempt_at DATETIME(6),
    processed_at DATETIME(6),
    failed_at DATETIME(6),
    last_error TEXT,
    status ENUM('PENDING', 'PUBLISHING', 'PROCESSED', 'FAILED'),
    PRIMARY KEY (id)
);

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'messages' AND column_name = 'source_event_id') = 0,
    'ALTER TABLE messages ADD COLUMN source_event_id BINARY(16)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'messages' AND index_name = 'uk_messages_source_event_id') = 0,
    'ALTER TABLE messages ADD CONSTRAINT uk_messages_source_event_id UNIQUE (source_event_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'outbox_events' AND column_name = 'publish_attempts') = 0,
    'ALTER TABLE outbox_events ADD COLUMN publish_attempts INT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'outbox_events' AND column_name = 'last_attempt_at') = 0,
    'ALTER TABLE outbox_events ADD COLUMN last_attempt_at DATETIME(6)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'outbox_events' AND column_name = 'processed_at') = 0,
    'ALTER TABLE outbox_events ADD COLUMN processed_at DATETIME(6)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'outbox_events' AND column_name = 'failed_at') = 0,
    'ALTER TABLE outbox_events ADD COLUMN failed_at DATETIME(6)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'outbox_events' AND column_name = 'last_error') = 0,
    'ALTER TABLE outbox_events ADD COLUMN last_error TEXT',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE outbox_events MODIFY COLUMN status ENUM('PENDING', 'PUBLISHING', 'PROCESSED', 'FAILED');
ALTER TABLE outbox_events MODIFY COLUMN publish_attempts INT NOT NULL DEFAULT 0;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'outbox_events' AND index_name = 'idx_outbox_events_status_created_at') = 0,
    'CREATE INDEX idx_outbox_events_status_created_at ON outbox_events (status, created_at)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'outbox_events' AND index_name = 'idx_outbox_events_status_last_attempt_at') = 0,
    'CREATE INDEX idx_outbox_events_status_last_attempt_at ON outbox_events (status, last_attempt_at)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
