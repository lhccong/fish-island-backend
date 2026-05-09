-- 朋友圈动态和评论新增置顶字段
-- @author cong

-- 动态表新增 isTop 字段（仅管理员可操作）
ALTER TABLE `moments`
    ADD COLUMN `isTop` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否，1-是' AFTER `commentNum`;

-- 评论表新增 isTop 字段（动态发布者或管理员可操作，仅顶级评论有效）
ALTER TABLE `moments_comment`
    ADD COLUMN `isTop` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否，1-是（仅顶级评论有效）' AFTER `content`;

-- 为置顶字段添加索引，优化排序查询性能
ALTER TABLE `moments` ADD INDEX `idx_isTop` (`isTop`);
ALTER TABLE `moments_comment` ADD INDEX `idx_isTop` (`isTop`);
