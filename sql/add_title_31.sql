-- 永久会员专属称号：闪耀永恒岛民（累计赞助满 29.9 元自动颁发）
INSERT INTO user_title (titleId, name, titleImg, isDelete)
VALUES (31, '闪耀永恒岛民', NULL, 0)
ON DUPLICATE KEY UPDATE name = '闪耀永恒岛民';
