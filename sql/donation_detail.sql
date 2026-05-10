-- 打赏明细记录表（每次打赏独立一条，不累加）
CREATE TABLE IF NOT EXISTS `donation_detail_records`
(
    `id`         BIGINT       NOT NULL COMMENT '明细记录ID',
    `userId`     BIGINT       NOT NULL COMMENT '打赏用户ID',
    `amount`     DECIMAL(15, 2) NOT NULL COMMENT '本次打赏金额（元）',
    `remark`     VARCHAR(512)            DEFAULT NULL COMMENT '打赏留言/备注',
    `isDelete`   TINYINT                 DEFAULT 0 NOT NULL COMMENT '是否删除',
    `createTime` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '打赏时间',
    `updateTime` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`userId`),
    INDEX `idx_create_time` (`createTime`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '打赏明细记录表（每次打赏独立一条，不累加）';
