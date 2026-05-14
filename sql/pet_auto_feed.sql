-- 宠物自动喂食配置表
CREATE TABLE IF NOT EXISTS `pet_auto_feed_config`
(
    `id`               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `userId`           BIGINT       NOT NULL COMMENT '用户ID',
    `petId`            BIGINT       NOT NULL COMMENT '宠物ID',
    `enabled`          TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用：0-关闭，1-开启',
    `foodCode`         VARCHAR(64)  NOT NULL COMMENT '使用的食物模板code，关联 item_templates.code',
    `triggerThreshold` INT          NOT NULL DEFAULT 30 COMMENT '触发喂食的饱食度阈值（低于此值时自动喂食，hunger越高越饱）',
    `createTime`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`         TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    UNIQUE KEY `uk_user_pet` (`userId`, `petId`),
    INDEX `idx_enabled` (`enabled`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='宠物自动喂食配置表';

-- 食物模板数据（插入到 item_templates）
INSERT INTO `item_templates` (`code`, `name`, `category`, `sub_type`, `rarity`, `levelReq`, `description`, `stackable`, `removePoint`, `purchasable`, `purchasePoint`, `mainAttr`, `icon`)
VALUES
    ('food_basic_01',   '普通鱼饼',   'consumable', 'food', 1, 1,  '喂食后恢复20点饥饿度，增加1点经验',                    1, 2,  1, 5,  '{"hungerRestore":20,"moodBonus":0,"expBonus":1}',   'https://oss.cqbo.com/moyu/food/fish_cake.webp'),
    ('food_medium_01',  '香煎鱼排',   'consumable', 'food', 2, 1,  '喂食后恢复40点饥饿度，恢复5点心情，增加3点经验',       1, 5,  1, 15, '{"hungerRestore":40,"moodBonus":5,"expBonus":3}',   'https://oss.cqbo.com/moyu/food/fish_steak.webp'),
    ('food_premium_01', '高级鱼宴',   'consumable', 'food', 3, 10, '喂食后恢复60点饥饿度，恢复10点心情，增加8点经验',      1, 10, 1, 40, '{"hungerRestore":60,"moodBonus":10,"expBonus":8}',  'https://oss.cqbo.com/moyu/food/fish_feast.webp'),
    ('food_luxury_01',  '顶级龙虾宴', 'consumable', 'food', 5, 20, '喂食后恢复100点饥饿度，恢复20点心情，增加20点经验',    1, 20, 1, 100,'{"hungerRestore":100,"moodBonus":20,"expBonus":20}','https://oss.cqbo.com/moyu/food/lobster_feast.webp');
