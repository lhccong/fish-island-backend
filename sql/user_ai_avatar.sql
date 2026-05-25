-- 用户 AI 分身表（与用户一对一）
CREATE TABLE IF NOT EXISTS user_ai_avatar
(
    id           BIGINT AUTO_INCREMENT COMMENT '分身ID' PRIMARY KEY,
    userId       BIGINT                             NOT NULL COMMENT '用户ID',
    avatarName   VARCHAR(64)                        NOT NULL COMMENT '分身名称',
    systemPrompt TEXT                               NULL COMMENT '分身系统提示词',
    enabled      TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否启用分身：0-关闭，1-开启',
    createTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete     TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除：0-未删除，1-已删除',
    UNIQUE INDEX uk_user_id (userId)
) COMMENT '用户AI分身表' COLLATE = utf8mb4_unicode_ci;
