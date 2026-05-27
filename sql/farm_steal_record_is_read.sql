-- 偷菜记录增加是否已读字段
ALTER TABLE `farm_steal_record`
    ADD COLUMN `isRead` tinyint NOT NULL DEFAULT 0 COMMENT '是否已读(0-未读、1-已读)' AFTER `coinGained`;
