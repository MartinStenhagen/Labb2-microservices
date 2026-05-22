SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'messages' AND column_name = 'room') = 0,
    'ALTER TABLE messages ADD COLUMN room VARCHAR(255) NOT NULL DEFAULT ''general''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'messages' AND index_name = 'idx_messages_room_created_at') = 0,
    'CREATE INDEX idx_messages_room_created_at ON messages (room, created_at)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
