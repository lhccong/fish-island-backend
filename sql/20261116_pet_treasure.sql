-- Run once before deploying camp treasure endpoints. No existing user data is modified.
CREATE TABLE IF NOT EXISTS pet_treasure_dig (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    dig_day DATE NOT NULL,
    slot INT NOT NULL,
    site_id VARCHAR(24) NOT NULL,
    ready_at BIGINT NOT NULL,
    reward INT NOT NULL,
    completed_at BIGINT NULL,
    UNIQUE KEY uk_treasure_daily_slot (user_id, dig_day, slot),
    KEY idx_treasure_latest (user_id, ready_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
