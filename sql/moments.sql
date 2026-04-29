-- 微信朋友圈相关表设计
-- @author fish-island

USE fish;

-- =============================================
-- 1. 朋友圈动态表
-- =============================================
CREATE TABLE IF NOT EXISTS moments
(
    id          BIGINT AUTO_INCREMENT COMMENT '动态ID' PRIMARY KEY,
    userId      BIGINT                             NOT NULL COMMENT '发布者用户ID',
    content     VARCHAR(2000)                      NULL COMMENT '文字内容',
    mediaJson   JSON                               NULL COMMENT '媒体资源列表（图片/视频），格式：[{type:"image",url:"..."},{type:"video",url:"...",cover:"..."}]',
    location    VARCHAR(128)                       NULL COMMENT '位置信息',
    visibility  TINYINT  DEFAULT 0                 NOT NULL COMMENT '可见范围：0-所有朋友，1-仅自己，2-部分可见，3-不给谁看',
    allowList   JSON                               NULL COMMENT '部分可见的用户ID列表（visibility=2时有效）',
    blockList   JSON                               NULL COMMENT '不给谁看的用户ID列表（visibility=3时有效）',
    likeNum     INT      DEFAULT 0                 NOT NULL COMMENT '点赞数',
    commentNum  INT      DEFAULT 0                 NOT NULL COMMENT '评论数',
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '发布时间',
    updateTime  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete    TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    INDEX idx_userId (userId),
    INDEX idx_createTime (createTime)
) COMMENT '朋友圈动态表' COLLATE = utf8mb4_unicode_ci;

-- =============================================
-- 2. 动态点赞表（硬删除）
-- =============================================
CREATE TABLE IF NOT EXISTS moments_like
(
    id         BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    momentId   BIGINT                             NOT NULL COMMENT '动态ID',
    userId     BIGINT                             NOT NULL COMMENT '点赞用户ID',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '点赞时间',
    UNIQUE KEY uk_moment_user (momentId, userId),
    INDEX idx_momentId (momentId),
    INDEX idx_userId (userId)
) COMMENT '朋友圈点赞表' COLLATE = utf8mb4_unicode_ci;

-- =============================================
-- 3. 动态评论表
-- =============================================
CREATE TABLE IF NOT EXISTS moments_comment
(
    id         BIGINT AUTO_INCREMENT COMMENT '评论ID' PRIMARY KEY,
    momentId   BIGINT                             NOT NULL COMMENT '动态ID',
    userId     BIGINT                             NOT NULL COMMENT '评论者用户ID',
    replyUserId BIGINT                            NULL COMMENT '被回复的用户ID（NULL表示直接评论动态）',
    parentId   BIGINT                             NULL COMMENT '父评论ID（NULL表示顶级评论）',
    content    VARCHAR(500)                       NOT NULL COMMENT '评论内容',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '评论时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    INDEX idx_momentId (momentId),
    INDEX idx_userId (userId),
    INDEX idx_parentId (parentId)
) COMMENT '朋友圈评论表' COLLATE = utf8mb4_unicode_ci;
