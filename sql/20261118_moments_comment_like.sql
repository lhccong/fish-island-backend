-- 在 add_top_field.sql 之后执行，既有评论的点赞数初始化为 0。
ALTER TABLE moments_comment
    ADD COLUMN likeNum INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    ADD INDEX idx_comment_rank (momentId, parentId, isDelete, isTop, likeNum, createTime, id);

CREATE TABLE IF NOT EXISTS moments_comment_like
(
    commentId BIGINT NOT NULL COMMENT '评论ID',
    userId    BIGINT NOT NULL COMMENT '点赞用户ID',
    PRIMARY KEY (commentId, userId)
) ENGINE = InnoDB COMMENT '朋友圈评论点赞关系';
