-- 独立记录已发送的点赞通知，取消点赞或删除通知后仍保留。
CREATE TABLE IF NOT EXISTS moments_comment_like_notice
(
    commentId BIGINT NOT NULL COMMENT '评论ID',
    userId    BIGINT NOT NULL COMMENT '点赞用户ID',
    PRIMARY KEY (commentId, userId)
) ENGINE = InnoDB COMMENT '朋友圈评论点赞通知去重记录';
