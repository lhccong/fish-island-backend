-- 数据源 Cookie 配置表（第三方热榜等抓取用）
CREATE TABLE IF NOT EXISTS datasource_cookie
(
    id             BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    dataSourceKey  VARCHAR(64)  NOT NULL COMMENT '数据源标识，对应 HotDataKeyEnum.value',
    cookieValue    TEXT         NOT NULL COMMENT 'Cookie 字符串',
    remark         VARCHAR(256) DEFAULT NULL COMMENT '备注',
    status         TINYINT      DEFAULT 1 NOT NULL COMMENT '状态：0-禁用 1-启用',
    createTime     DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime     DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete       TINYINT      DEFAULT 0 NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_data_source_key (dataSourceKey),
    INDEX idx_status (status)
) COMMENT '数据源 Cookie 配置表' COLLATE = utf8mb4_unicode_ci;
