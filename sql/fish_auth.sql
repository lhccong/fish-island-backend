-- fish_auth 第三方应用表（OAuth2 客户端）
CREATE TABLE IF NOT EXISTS fish_auth
(
    id          BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    appName     VARCHAR(128)                        NOT NULL COMMENT '应用名称',
    appWebsite  VARCHAR(512)                        NULL COMMENT '应用网站地址',
    appDesc     VARCHAR(1024)                       NULL COMMENT '应用描述',
    redirectUri VARCHAR(1024)                       NOT NULL COMMENT '回调地址（多个用逗号分隔）',
    clientId    VARCHAR(64)                         NOT NULL COMMENT 'Client ID',
    clientSecret VARCHAR(128)                       NOT NULL COMMENT 'Client Secret（加密存储）',
    userId      BIGINT                              NOT NULL COMMENT '创建者用户 ID',
    status      TINYINT  DEFAULT 1                  NOT NULL COMMENT '状态：0-禁用，1-启用',
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP  NOT NULL COMMENT '创建时间',
    updateTime  DATETIME DEFAULT CURRENT_TIMESTAMP  NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete    TINYINT  DEFAULT 0                  NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_clientId (clientId)
) COMMENT '第三方应用（OAuth2 客户端）' COLLATE = utf8mb4_unicode_ci;

-- fish_auth_code 授权码表（临时，5 分钟过期）
CREATE TABLE IF NOT EXISTS fish_auth_code
(
    id          BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    code        VARCHAR(128)                        NOT NULL COMMENT '授权码',
    clientId    VARCHAR(64)                         NOT NULL COMMENT 'Client ID',
    userId      BIGINT                              NOT NULL COMMENT '授权用户 ID',
    redirectUri VARCHAR(1024)                       NOT NULL COMMENT '回调地址',
    scope       VARCHAR(256)  DEFAULT 'read'        NOT NULL COMMENT '授权范围',
    used        TINYINT       DEFAULT 0             NOT NULL COMMENT '是否已使用：0-未使用，1-已使用',
    expireTime  DATETIME                            NOT NULL COMMENT '过期时间',
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP  NOT NULL COMMENT '创建时间',
    UNIQUE KEY uk_code (code),
    INDEX idx_clientId (clientId)
) COMMENT 'OAuth2 授权码' COLLATE = utf8mb4_unicode_ci;
