-- Apply after 20261119_chat_bubble.sql. Existing account entitlements remain keyed by bubble_id.
CREATE TABLE IF NOT EXISTS chat_bubble_product (
    id VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    points INT NOT NULL,
    image_url VARCHAR(1024) NULL,
    dark_image_url VARCHAR(1024) NULL,
    text_tone VARCHAR(8) NOT NULL DEFAULT 'theme',
    sort_order INT NOT NULL DEFAULT 100,
    CONSTRAINT chk_chat_bubble_product_points CHECK (points > 0),
    CONSTRAINT chk_chat_bubble_product_tone CHECK (text_tone IN ('theme', 'light', 'dark'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO chat_bubble_product (id, name, points, sort_order) VALUES
    ('tide-letter', '潮汐来信', 300, 10),
    ('berry-cat-paw', '草莓猫爪', 300, 20),
    ('moon-jellyfish', '月光水母', 300, 30),
    ('yuzu-capybara', '柚子水豚', 300, 40),
    ('sea-salt-seal', '海盐小海豹', 300, 50),
    ('black-sugar-jelly', '黑糖星冻', 300, 60)
ON DUPLICATE KEY UPDATE id = chat_bubble_product.id;
