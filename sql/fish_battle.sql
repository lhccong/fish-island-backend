-- 摸鱼大乱斗游戏房间表
CREATE TABLE IF NOT EXISTS `fish_battle_room` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `room_code` VARCHAR(10) NOT NULL COMMENT '房间编码（展示用/邀请码）',
    `room_name` VARCHAR(50) NOT NULL DEFAULT '摸鱼大乱斗房间' COMMENT '房间名称',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '房间状态（0等待中/1选英雄中/2对局中/3已结束）',
    `game_mode` VARCHAR(20) NOT NULL DEFAULT 'classic' COMMENT '游戏模式',
    `max_players` INT NOT NULL DEFAULT 10 COMMENT '最大人数',
    `current_players` INT NOT NULL DEFAULT 0 COMMENT '当前人数',
    `ai_fill_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否开启AI补位（0否/1是）',
    `creator_id` BIGINT NOT NULL COMMENT '创建者用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除（0否/1是）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_room_code` (`room_code`),
    KEY `idx_status` (`status`),
    KEY `idx_creator_id` (`creator_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='摸鱼大乱斗游戏房间表';

-- 摸鱼大乱斗房间玩家表
CREATE TABLE IF NOT EXISTS `fish_battle_room_player` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `room_id` BIGINT NOT NULL COMMENT '房间ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID（AI玩家为NULL）',
    `player_name` VARCHAR(50) DEFAULT NULL COMMENT '玩家名称',
    `team` VARCHAR(10) DEFAULT NULL COMMENT '队伍（blue/red）',
    `is_ready` TINYINT NOT NULL DEFAULT 0 COMMENT '是否准备（0否/1是）',
    `is_ai` TINYINT NOT NULL DEFAULT 0 COMMENT '是否为AI（0否/1是）',
    `hero_id` VARCHAR(50) DEFAULT NULL COMMENT '分配的英雄ID',
    `skin_id` VARCHAR(50) DEFAULT NULL COMMENT '选择的皮肤ID',
    `spell1` VARCHAR(50) DEFAULT NULL COMMENT '召唤师技能1',
    `spell2` VARCHAR(50) DEFAULT NULL COMMENT '召唤师技能2',
    `slot_index` INT DEFAULT NULL COMMENT '队伍中的位置索引（0-4）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_user_id` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='摸鱼大乱斗房间玩家表';

-- 摸鱼大乱斗英雄表（独立于现有hero表）
CREATE TABLE IF NOT EXISTS `fish_battle_hero` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `hero_id`       VARCHAR(32)     NOT NULL COMMENT '英雄唯一标识（如 ashe）',
    `name`          VARCHAR(32)     NOT NULL COMMENT '中文名（如 艾希）',
    `name_en`       VARCHAR(32)     NOT NULL COMMENT '英文名（如 Ashe）',
    `role`          VARCHAR(16)     NOT NULL COMMENT '职业类型（tank/fighter/mage/marksman/support）',
    `base_hp`       INT             NOT NULL DEFAULT 0 COMMENT '基础生命值',
    `base_mp`       INT             NOT NULL DEFAULT 0 COMMENT '基础法力值',
    `base_ad`       INT             NOT NULL DEFAULT 0 COMMENT '基础物理攻击力',
    `move_speed`    INT             NOT NULL DEFAULT 0 COMMENT '基础移动速度',
    `attack_range`  DECIMAL(4, 1)   NOT NULL DEFAULT 0.0 COMMENT '基础攻击距离',
    `attack_speed`  DECIMAL(3, 2)   NOT NULL DEFAULT 0.00 COMMENT '基础攻击速度',
    `avatar_url`    VARCHAR(512)    DEFAULT NULL COMMENT '英雄头像URL',
    `splash_art`    VARCHAR(512)    DEFAULT NULL COMMENT '英雄立绘URL（选英雄界面大图展示用）',
    `model_url`     VARCHAR(512)    DEFAULT NULL COMMENT '3D模型URL',
    `asset_config`  LONGTEXT DEFAULT NULL COMMENT '英雄展示资产配置JSON',
    `skills`        TEXT            DEFAULT NULL COMMENT '技能JSON（Q/W/E/R名称+图标+描述）',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '状态（0禁用/1启用）',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hero_id` (`hero_id`),
    KEY `idx_role` (`role`),
    KEY `idx_status` (`status`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '摸鱼大乱斗英雄表';


-- 召唤师技能表
CREATE TABLE IF NOT EXISTS `fish_battle_summoner_spell` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `spell_id` VARCHAR(32) NOT NULL COMMENT '技能唯一标识（如 flash, heal, ignite）',
    `name` VARCHAR(64) NOT NULL COMMENT '中文名',
    `icon` VARCHAR(512) DEFAULT NULL COMMENT '图标URL或emoji',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '技能描述',
    `cooldown` INT DEFAULT 0 COMMENT '冷却时间(秒)',
    `status` TINYINT DEFAULT 1 COMMENT '状态（0禁用/1启用）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_spell_id` (`spell_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='摸鱼大乱斗召唤师技能表';


-- 英雄皮肤表
CREATE TABLE IF NOT EXISTS `fish_battle_hero_skin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `hero_id` VARCHAR(32) NOT NULL COMMENT '关联英雄标识',
    `skin_id` VARCHAR(64) NOT NULL COMMENT '皮肤唯一标识（如 yasuo_default）',
    `skin_name` VARCHAR(128) NOT NULL COMMENT '皮肤名称',
    `splash_art` VARCHAR(512) DEFAULT NULL COMMENT '皮肤立绘URL',
    `model_url` VARCHAR(512) DEFAULT NULL COMMENT '皮肤3D模型URL',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认皮肤（1是/0否）',
    `status` TINYINT DEFAULT 1 COMMENT '状态（0禁用/1启用）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_skin_id` (`skin_id`),
    KEY `idx_hero_id` (`hero_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='摸鱼大乱斗英雄皮肤表';


-- 对局记录表
CREATE TABLE IF NOT EXISTS `fish_battle_game` (
                                                  `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                                  `room_id`           BIGINT          NOT NULL COMMENT '关联房间ID',
                                                  `game_mode`         VARCHAR(16)     NOT NULL DEFAULT 'classic' COMMENT '游戏模式',
    `winning_team`      VARCHAR(8)      DEFAULT NULL COMMENT '胜利队伍（blue/red）',
    `blue_kills`        INT             NOT NULL DEFAULT 0 COMMENT '蓝队总击杀',
    `red_kills`         INT             NOT NULL DEFAULT 0 COMMENT '红队总击杀',
    `duration_seconds`  INT             NOT NULL DEFAULT 0 COMMENT '对局时长（秒）',
    `end_reason`        VARCHAR(32)     DEFAULT NULL COMMENT '结束原因（crystal_destroyed/kill_limit）',
    `mvp_user_id`       BIGINT          DEFAULT NULL COMMENT 'MVP用户ID',
    `start_time`        DATETIME        DEFAULT NULL COMMENT '开始时间',
    `end_time`          DATETIME        DEFAULT NULL COMMENT '结束时间',
    `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_mvp_user_id` (`mvp_user_id`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_create_time` (`create_time`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '摸鱼大乱斗对局记录表';

-- 玩家对局统计表（单局）
CREATE TABLE IF NOT EXISTS `fish_battle_player_stats` (
                                                          `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                                          `game_id`           BIGINT          NOT NULL COMMENT '对局ID',
                                                          `user_id`           BIGINT          NOT NULL COMMENT '用户ID',
                                                          `hero_id`           VARCHAR(32)     NOT NULL COMMENT '使用的英雄ID',
    `team`              VARCHAR(8)      NOT NULL COMMENT '所在队伍（blue/red）',
    `kills`             INT             NOT NULL DEFAULT 0 COMMENT '击杀数',
    `deaths`            INT             NOT NULL DEFAULT 0 COMMENT '死亡数',
    `assists`           INT             NOT NULL DEFAULT 0 COMMENT '助攻数',
    `damage_dealt`      INT             NOT NULL DEFAULT 0 COMMENT '输出伤害',
    `damage_taken`      INT             NOT NULL DEFAULT 0 COMMENT '承受伤害',
    `healing`           INT             NOT NULL DEFAULT 0 COMMENT '治疗量',
    `is_mvp`            TINYINT         NOT NULL DEFAULT 0 COMMENT '是否MVP（0否/1是）',
    `is_win`            TINYINT         NOT NULL DEFAULT 0 COMMENT '是否胜利（0否/1是）',
    `likes`             INT             NOT NULL DEFAULT 0 COMMENT '获赞数',
    `points_earned`     INT             NOT NULL DEFAULT 0 COMMENT '本局获得积分',
    `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_game_id` (`game_id`),
    KEY `idx_user_id` (`user_id`),
    UNIQUE KEY `uk_game_user` (`game_id`, `user_id`),
    KEY `idx_hero_id` (`hero_id`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '摸鱼大乱斗玩家对局统计表';

-- 玩家总体统计表
CREATE TABLE IF NOT EXISTS `fish_battle_user_stats` (
                                                        `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                                        `user_id`           BIGINT          NOT NULL COMMENT '用户ID',
                                                        `total_games`       INT             NOT NULL DEFAULT 0 COMMENT '总场次',
                                                        `wins`              INT             NOT NULL DEFAULT 0 COMMENT '胜场',
                                                        `losses`            INT             NOT NULL DEFAULT 0 COMMENT '败场',
                                                        `total_kills`       INT             NOT NULL DEFAULT 0 COMMENT '总击杀',
                                                        `total_deaths`      INT             NOT NULL DEFAULT 0 COMMENT '总死亡',
                                                        `total_assists`     INT             NOT NULL DEFAULT 0 COMMENT '总助攻',
                                                        `mvp_count`         INT             NOT NULL DEFAULT 0 COMMENT 'MVP次数',
                                                        `current_streak`    INT             NOT NULL DEFAULT 0 COMMENT '当前连胜（负数表示连败）',
                                                        `max_streak`        INT             NOT NULL DEFAULT 0 COMMENT '最大连胜',
                                                        `today_games`       INT             NOT NULL DEFAULT 0 COMMENT '今日已玩场次',
                                                        `today_date`        DATE            DEFAULT NULL COMMENT '今日日期（用于每日重置计数）',
                                                        `daily_limit`       INT             NOT NULL DEFAULT 20 COMMENT '每日对局上限',
                                                        `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                        `update_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                        PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '摸鱼大乱斗玩家总体统计表';


-- 英雄数据
INSERT INTO `fish`.`fish_battle_hero` (`id`, `hero_id`, `name`, `name_en`, `role`, `base_hp`, `base_mp`, `base_ad`, `move_speed`, `attack_range`, `attack_speed`, `avatar_url`, `splash_art`, `model_url`, `asset_config`, `skills`, `status`, `create_time`, `update_time`) VALUES (1, 'ashe', '艾希', 'Ashe', 'marksman', 700, 500, 70, 310, 10.0, 1.00, 'https://ddragon.leagueoflegends.com/cdn/14.3.1/img/champion/Ashe.png', 'https://ddragon.leagueoflegends.com/cdn/img/champion/loading/Ashe_0.jpg', 'https://cdn.xiaojingge.com/3d-battle/models/heroes/ashe/%E5%AF%92%E5%86%B0%E5%B0%84%E6%89%8B.glb', '{}', '{\"q\":{\"name\":\"游侠集中\",\"icon\":\"q\",\"description\":\"艾希的普攻变为连射箭矢，增加攻速和伤害\"},\"w\":{\"name\":\"万箭齐发\",\"icon\":\"w\",\"description\":\"艾希发射一排锥形箭雨，伤害并减速命中的敌人\"},\"e\":{\"name\":\"鹰击长空\",\"icon\":\"e\",\"description\":\"艾希派出一只鹰灵探查目标区域，提供视野\"},\"r\":{\"name\":\"魔法水晶箭\",\"icon\":\"r\",\"description\":\"艾希射出一支巨大冰箭，击晕命中的第一个敌方英雄，飞行距离越远晕眩越久\"}}', 1, '2026-04-27 22:09:31', '2026-04-27 23:46:49');

-- 召唤师技能数据
INSERT INTO `fish`.`fish_battle_summoner_spell` (`id`, `spell_id`, `name`, `icon`, `description`, `cooldown`, `status`, `create_time`, `update_time`) VALUES (1, 'flash', '闪现', 'https://ddragon.leagueoflegends.com/cdn/14.3.1/img/spell/SummonerFlash.png', '瞬间传送到附近位置', 300, 1, '2026-04-27 22:09:21', '2026-04-27 23:06:14');
INSERT INTO `fish`.`fish_battle_summoner_spell` (`id`, `spell_id`, `name`, `icon`, `description`, `cooldown`, `status`, `create_time`, `update_time`) VALUES (2, 'heal', '治疗', 'https://ddragon.leagueoflegends.com/cdn/14.3.1/img/spell/SummonerHeal.png', '恢复自身和附近友军生命值', 240, 1, '2026-04-27 22:09:21', '2026-04-27 23:06:15');
INSERT INTO `fish`.`fish_battle_summoner_spell` (`id`, `spell_id`, `name`, `icon`, `description`, `cooldown`, `status`, `create_time`, `update_time`) VALUES (3, 'ghost', '疾跑', 'https://ddragon.leagueoflegends.com/cdn/14.3.1/img/spell/SummonerHaste.png', '大幅提升移动速度', 210, 1, '2026-04-27 22:09:21', '2026-04-27 23:07:50');

-- 皮肤数据
INSERT INTO `fish`.`fish_battle_hero_skin` (`id`, `hero_id`, `skin_id`, `skin_name`, `splash_art`, `model_url`, `is_default`, `status`, `create_time`, `update_time`) VALUES (1, 'ashe', 'ashe_1', '极地女神 艾希', 'https://ddragon.leagueoflegends.com/cdn/img/champion/loading/Ashe_1.jpg', NULL, 0, 1, '2026-04-28 00:06:14', '2026-04-28 00:06:14');