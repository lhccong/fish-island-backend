CREATE TABLE IF NOT EXISTS user_chat_bubble (
    user_id BIGINT NOT NULL,
    bubble_id VARCHAR(32) NOT NULL,
    expires_at BIGINT NOT NULL,
    equipped TINYINT NULL DEFAULT NULL COMMENT '1 for selected; NULL otherwise',
    PRIMARY KEY (user_id, bubble_id),
    UNIQUE KEY uk_user_chat_bubble_equipped (user_id, equipped)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
