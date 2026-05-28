SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'messages' AND column_name = 'bot_personality') = 0,
    'ALTER TABLE messages ADD COLUMN bot_personality VARCHAR(32) NOT NULL DEFAULT ''neutral''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
