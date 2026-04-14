-- 用户备注表
CREATE TABLE IF NOT EXISTS user_remark
(
    id          BIGINT AUTO_INCREMENT COMMENT '备注ID' PRIMARY KEY,
    userId     BIGINT                            NOT NULL COMMENT '用户ID',
    content     VARCHAR(512)                      NULL COMMENT '备注内容',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   TINYINT  DEFAULT 0               NOT NULL COMMENT '是否删除：0-未删除，1-已删除',
    UNIQUE INDEX uk_user_id (userId),
    INDEX idx_create_time (createTime)
) COMMENT '用户备注表' COLLATE = utf8mb4_unicode_ci;
