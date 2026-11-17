-- Deploy before starting the updated backend. Existing values default to no suffix.
ALTER TABLE `user` ADD COLUMN `pokeSuffix` varchar(30) NOT NULL DEFAULT '' COMMENT '被戳后缀';
