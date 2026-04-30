-- 用户表新增朋友圈背景图字段
ALTER TABLE `user` ADD COLUMN `momentsBgUrl` VARCHAR(1024) NULL COMMENT '朋友圈背景图' AFTER `titleIdList`;
