ALTER TABLE emoticon_favour
    ADD COLUMN ocrText TEXT NULL COMMENT '表情包识别文字',
    ADD COLUMN ocrAttempts TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '识别重试次数',
    ADD COLUMN ocrNextAttemptAt DATETIME(3) NULL COMMENT '识别下次执行时间',
    ADD INDEX idx_emoticon_ocr_due (ocrNextAttemptAt, id);
