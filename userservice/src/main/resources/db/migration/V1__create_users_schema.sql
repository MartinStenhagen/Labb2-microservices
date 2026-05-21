CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username)
);
