-- 爬塔记录表
CREATE TABLE IF NOT EXISTS tower_climb_record
(
    id          BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    userId      BIGINT                             NOT NULL COMMENT '用户ID',
    floor       INT        DEFAULT 1               NOT NULL COMMENT '当前挑战层数',
    maxFloor    INT        DEFAULT 0               NOT NULL COMMENT '历史最高通关层数',
    result      TINYINT    DEFAULT 0               NOT NULL COMMENT '本次挑战结果：0-失败，1-胜利',
    petLevel    INT        DEFAULT 1               NOT NULL COMMENT '挑战时宠物等级',
    petHpLeft   INT        DEFAULT 0               NOT NULL COMMENT '挑战结束时宠物剩余血量',
    rewardPoints INT       DEFAULT 0               NOT NULL COMMENT '本次获得积分奖励',
    createTime  DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime  DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete    TINYINT    DEFAULT 0               NOT NULL COMMENT '是否删除',
    INDEX idx_user_id (userId),
    INDEX idx_floor (floor),
    INDEX idx_max_floor (maxFloor)
) COMMENT = '爬塔挑战记录表' COLLATE = utf8mb4_unicode_ci;

-- 用户爬塔进度表（记录每个用户当前最高层）
CREATE TABLE IF NOT EXISTS tower_climb_progress
(
    id          BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    userId      BIGINT                             NOT NULL COMMENT '用户ID',
    maxFloor    INT        DEFAULT 0               NOT NULL COMMENT '历史最高通关层数',
    createTime  DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime  DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete    TINYINT    DEFAULT 0               NOT NULL COMMENT '是否删除',
    UNIQUE INDEX uk_user_id (userId)
) COMMENT = '用户爬塔进度表' COLLATE = utf8mb4_unicode_ci;
