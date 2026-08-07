-- 游戏房间表
CREATE TABLE IF NOT EXISTS `game_room` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `room_id` VARCHAR(20) NOT NULL UNIQUE COMMENT '房间ID',
    `game_type` INT NOT NULL COMMENT '游戏类型 1-斗地主 2-癞子版 3-技能版 4-跑得快 5-德州扑克 6-麻将 7-骗子酒馆 8-谁是卧底 9-Uno',
    `room_state` INT DEFAULT 1 COMMENT '房间状态 1-等待中 2-已准备 3-发牌中 4-叫地主 5-游戏中 6-结束中 0-已关闭',
    `creator_id` BIGINT NOT NULL COMMENT '创建者用户ID',
    `creator_name` VARCHAR(50) DEFAULT '' COMMENT '创建者名称',
    `max_players` INT DEFAULT 3 COMMENT '最大玩家数',
    `current_players` INT DEFAULT 0 COMMENT '当前玩家数',
    `password` VARCHAR(50) DEFAULT NULL COMMENT '房间密码',
    `config` TEXT COMMENT '房间配置 JSON',
    `enable_lai_zi` TINYINT(1) DEFAULT 0 COMMENT '是否启用癞子模式',
    `enable_skill` TINYINT(1) DEFAULT 0 COMMENT '是否启用技能模式',
    `enable_dont_shuffle` TINYINT(1) DEFAULT 0 COMMENT '是否不洗牌模式',
    `enable_chat` TINYINT(1) DEFAULT 1 COMMENT '是否允许聊天',
    `show_ip` TINYINT(1) DEFAULT 0 COMMENT '是否显示IP',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_game_type_state` (`game_type`, `room_state`),
    INDEX `idx_creator_id` (`creator_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏房间表';

-- 游戏记录表
CREATE TABLE IF NOT EXISTS `game_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `room_id` VARCHAR(20) NOT NULL COMMENT '房间ID',
    `game_type` INT NOT NULL COMMENT '游戏类型',
    `winner_id` BIGINT DEFAULT NULL COMMENT '获胜者用户ID',
    `winner_name` VARCHAR(50) DEFAULT '' COMMENT '获胜者名称',
    `players` TEXT COMMENT '玩家列表 JSON',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `duration` INT DEFAULT 0 COMMENT '游戏时长(秒)',
    `details` TEXT COMMENT '游戏详情 JSON',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_room_id` (`room_id`),
    INDEX `idx_game_type` (`game_type`),
    INDEX `idx_winner_id` (`winner_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏记录表';

-- 玩家游戏统计表
CREATE TABLE IF NOT EXISTS `game_statistics` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `game_type` INT NOT NULL COMMENT '游戏类型',
    `total_games` INT DEFAULT 0 COMMENT '总局数',
    `win_games` INT DEFAULT 0 COMMENT '获胜局数',
    `lose_games` INT DEFAULT 0 COMMENT '失败局数',
    `max_consecutive_wins` INT DEFAULT 0 COMMENT '最大连胜',
    `max_consecutive_loses` INT DEFAULT 0 COMMENT '最大连败',
    `current_consecutive_wins` INT DEFAULT 0 COMMENT '当前连胜',
    `current_consecutive_loses` INT DEFAULT 0 COMMENT '当前连败',
    `total_play_time` BIGINT DEFAULT 0 COMMENT '总游戏时长(秒)',
    `last_play_time` DATETIME DEFAULT NULL COMMENT '最后游戏时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_user_game` (`user_id`, `game_type`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_game_type` (`game_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='玩家游戏统计表';
