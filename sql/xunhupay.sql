-- 虎皮椒支付订单记录表
CREATE TABLE IF NOT EXISTS `pay_order`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `userId`           BIGINT       NOT NULL COMMENT '用户ID',
    `tradeOrderId`     VARCHAR(64)  NOT NULL COMMENT '商户订单号（本系统生成）',
    `transactionId`    VARCHAR(64)  DEFAULT NULL COMMENT '支付平台交易号',
    `openOrderId`      VARCHAR(64)  DEFAULT NULL COMMENT '虎皮椒内部订单号',
    `title`            VARCHAR(128) NOT NULL COMMENT '订单标题',
    `totalFee`         DECIMAL(10, 2) NOT NULL COMMENT '订单金额（元）',
    `status`           VARCHAR(8)   NOT NULL DEFAULT 'PENDING' COMMENT '订单状态：PENDING-待支付，OD-已支付，CD-已退款，RD-退款中，UD-退款失败，CLOSED-已关闭',
    `notifyUrl`        VARCHAR(256) DEFAULT NULL COMMENT '回调通知地址',
    `returnUrl`        VARCHAR(256) DEFAULT NULL COMMENT '支付成功跳转地址',
    `attach`           TEXT         DEFAULT NULL COMMENT '备注/附加数据，回调时原样返回',
    `urlQrcode`        VARCHAR(512) DEFAULT NULL COMMENT '支付二维码地址（PC端）',
    `payUrl`           VARCHAR(512) DEFAULT NULL COMMENT '支付跳转链接（手机端）',
    `notifyTime`       DATETIME     DEFAULT NULL COMMENT '回调通知时间',
    `createTime`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`         TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trade_order_id` (`tradeOrderId`),
    KEY `idx_user_id` (`userId`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '虎皮椒支付订单记录表';
