-- 内容举报表
USE fish;

CREATE TABLE IF NOT EXISTS content_report
(
    id           BIGINT AUTO_INCREMENT COMMENT '举报ID' PRIMARY KEY,
    reporterId   BIGINT                             NOT NULL COMMENT '举报人用户ID',
    reportType   TINYINT                            NOT NULL COMMENT '举报类型：1-聊天记录，2-帖子，3-鱼小圈',
    targetId     BIGINT                             NOT NULL COMMENT '被举报对象ID（消息ID/帖子ID/动态ID）',
    targetUserId BIGINT                             NULL COMMENT '被举报用户ID',
    reasonType   TINYINT                            NOT NULL COMMENT '举报原因类型：1-18',
    description  VARCHAR(1000)                      NULL COMMENT '补充说明',
    status       TINYINT  DEFAULT 0                 NOT NULL COMMENT '处理状态：0-待处理，1-已处理，2-已驳回',
    handlerId    BIGINT                             NULL COMMENT '处理人ID',
    handleRemark VARCHAR(500)                       NULL COMMENT '处理备注',
    handleTime   DATETIME                           NULL COMMENT '处理时间',
    createTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete     TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除：0-未删除，1-已删除',
    INDEX idx_reporterId (reporterId),
    INDEX idx_target (reportType, targetId),
    INDEX idx_status (status),
    INDEX idx_createTime (createTime)
) COMMENT '内容举报表' COLLATE = utf8mb4_unicode_ci;

-- 若已执行过包含 roomId 的旧版本，可执行以下语句删除该字段
-- ALTER TABLE content_report DROP COLUMN roomId;
