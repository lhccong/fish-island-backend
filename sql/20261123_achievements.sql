CREATE TABLE IF NOT EXISTS user_achievement_reward (
    user_id BIGINT NOT NULL,
    achievement_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    subject_id INT NOT NULL,
    claimed_tiers INT NOT NULL,
    PRIMARY KEY (user_id, achievement_id, subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号各成就已领取的累计档数';
