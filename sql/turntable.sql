-- 转盘表
CREATE TABLE IF NOT EXISTS turntable
(
    id             BIGINT AUTO_INCREMENT COMMENT '转盘 ID' PRIMARY KEY,
    type           INT COMMENT '转盘类型 1-宠物装备转盘 2-称号转盘',
    name           VARCHAR(256) COMMENT '转盘名称',
    costPoints     INT      DEFAULT 1 COMMENT '每次抽奖消耗积分',
    guaranteeCount INT      DEFAULT 300 COMMENT '保底触发次数，0 表示无保底',
    status         INT      DEFAULT 1 COMMENT '1启用 0禁用',
    createTime     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete       TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除'
) COMMENT '转盘表' COLLATE = utf8mb4_unicode_ci;

-- 转盘奖励表
CREATE TABLE IF NOT EXISTS turntable_prize
(
    id           BIGINT AUTO_INCREMENT COMMENT '奖励ID' PRIMARY KEY,
    turntableId  BIGINT COMMENT '转盘 ID',
    prizeId      BIGINT COMMENT '物品 ID',
    quality      INT NOT NULL COMMENT '奖励品质 1-普通(N) 2-稀有(R) 3-史诗(SR) 4-传说(SSR)',
    prizeType    INT COMMENT '物品类型 1-装备 2-称号',
    probability  INT NOT NULL COMMENT '概率权重，总权重1000 ',
    stock        INT DEFAULT -1 COMMENT '奖品数量 -1表示无限',
    createTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete     TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除'
) COMMENT '奖项表' COLLATE = utf8mb4_unicode_ci;

-- 抽奖记录表
CREATE TABLE IF NOT EXISTS turntable_draw_record
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '抽奖记录ID',
    userId           BIGINT NOT NULL COMMENT '用户ID',
    turntableId      BIGINT NOT NULL COMMENT '转盘 ID',
    turntablePrizeId BIGINT NOT NULL COMMENT '装盘奖励ID',
    name             VARCHAR(128) COMMENT '奖品名称',
    prizeType        INT COMMENT '奖励类型 1-装备 2-称号',
    prizeId          BIGINT COMMENT '奖励ID',
    quality          INT COMMENT '奖励品质',
    costPoints       INT COMMENT '本次消耗积分',
    isGuarantee      TINYINT DEFAULT 0 COMMENT '是否触发保底 0-否 1-是',
    guaranteeType    INT COMMENT '保底类型 1-小保底 2-大保底',
    createTime       DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime       DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete         TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除 0-否 1-是'
) COMMENT '转盘抽奖记录表' COLLATE = utf8mb4_unicode_ci;

-- 用户转盘进度表
CREATE TABLE IF NOT EXISTS turntable_user_progress
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户转盘进度ID',
    userId         BIGINT NOT NULL COMMENT '用户ID',
    turntableId    BIGINT NOT NULL COMMENT '转盘ID',
    smallFailCount INT DEFAULT 0 COMMENT '小保底失败次数 10次小保底命中清零',
    totalDrawCount INT DEFAULT 0 COMMENT '累计抽奖次数 大保底命中重置或减300',
    guaranteeCount INT DEFAULT 300 COMMENT '保底阈值（冗余快照）',
    lastDrawTime   DATETIME COMMENT '上次抽奖时间',
    createTime     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete       TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除 0-否 1-是',
    UNIQUE KEY uk_user_turntable (userId, turntableId)
) COMMENT '用户转盘进度表' COLLATE = utf8mb4_unicode_ci;

-- 添加索引
CREATE INDEX idx_turntable_prize_turntableId ON turntable_prize(turntableId);
CREATE INDEX idx_draw_record_userId ON turntable_draw_record(userId);
CREATE INDEX idx_draw_record_turntableId ON turntable_draw_record(turntableId);
CREATE INDEX idx_user_progress_userId ON turntable_user_progress(userId);
