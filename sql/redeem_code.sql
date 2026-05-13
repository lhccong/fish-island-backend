-- 兑换码表
CREATE TABLE IF NOT EXISTS redeem_code
(
    id           BIGINT AUTO_INCREMENT COMMENT '兑换码ID' PRIMARY KEY,
    code         VARCHAR(64)  NOT NULL COMMENT '兑换码（唯一）',
    type         TINYINT      NOT NULL DEFAULT 1 COMMENT '类型：1-通用码（每人限领一次）2-专属码（一次性，仅限指定用户或先到先得）',
    targetUserId BIGINT       DEFAULT NULL COMMENT '专属码绑定的目标用户ID，NULL表示不限定用户（先到先得）',
    rewardType   TINYINT      NOT NULL COMMENT '奖励类型：1-积分 2-会员天数 3-道具 4-称号 5-头像框',
    rewardValue  BIGINT       NOT NULL COMMENT '奖励值：积分数量/会员天数/道具ID/称号ID/头像框ID',
    rewardCount  INT          DEFAULT 1 COMMENT '奖励数量（道具类有效）',
    description  VARCHAR(256) DEFAULT NULL COMMENT '兑换码描述/备注',
    expireTime   DATETIME     DEFAULT NULL COMMENT '过期时间，NULL表示永不过期',
    status       TINYINT      DEFAULT 1 COMMENT '状态：0-已禁用 1-正常 2-已用完',
    usedCount    INT          DEFAULT 0 COMMENT '已使用次数',
    maxUseCount  INT          DEFAULT -1 COMMENT '最大使用次数：-1不限（通用码），专属码固定为1',
    createTime   DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime   DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete     TINYINT      DEFAULT 0 NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_code (code),
    INDEX idx_type (type),
    INDEX idx_target_user (targetUserId),
    INDEX idx_status (status)
) COMMENT '兑换码表' COLLATE = utf8mb4_unicode_ci;

-- 兑换码使用记录表
CREATE TABLE IF NOT EXISTS redeem_code_record
(
    id          BIGINT AUTO_INCREMENT COMMENT '记录ID' PRIMARY KEY,
    codeId      BIGINT      NOT NULL COMMENT '兑换码ID',
    code        VARCHAR(64) NOT NULL COMMENT '兑换码（冗余，方便查询）',
    userId      BIGINT      NOT NULL COMMENT '使用者用户ID',
    rewardType  TINYINT     NOT NULL COMMENT '奖励类型（冗余快照）',
    rewardValue BIGINT      NOT NULL COMMENT '奖励值（冗余快照）',
    rewardCount INT         DEFAULT 1 COMMENT '奖励数量（冗余快照）',
    createTime  DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '兑换时间',
    updateTime  DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete    TINYINT     DEFAULT 0 NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_code_user (codeId, userId),
    INDEX idx_user_id (userId),
    INDEX idx_code_id (codeId)
) COMMENT '兑换码使用记录表' COLLATE = utf8mb4_unicode_ci;
