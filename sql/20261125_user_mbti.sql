-- Deploy before starting the updated backend. Existing users default to no MBTI.
ALTER TABLE `user` ADD COLUMN `mbti` varchar(4) NOT NULL DEFAULT '' COMMENT 'MBTI 类型';
