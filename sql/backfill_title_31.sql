-- =============================================================================
-- 批量补发称号 31「闪耀永恒岛民」+ event_remind 系统通知
-- 条件：累计赞助 amount >= 29.9，且尚未拥有称号 31
-- 通知字段与 EventRemindServiceImpl.sendSystemNotify 保持一致：
--   senderId=-1, sourceId=-1, sourceType=0, action=sponsor_title_31, state=0, url=''
-- 执行前请先跑 add_title_31.sql，预览无误后再 COMMIT
-- =============================================================================

-- USE fish;

-- 0. 确保称号元数据存在
INSERT INTO user_title (titleId, name, titleImg, isDelete)
VALUES (31, '闪耀永恒岛民', NULL, 0)
ON DUPLICATE KEY UPDATE name = '闪耀永恒岛民';

-- 1. 预览：即将补发称号的用户
SELECT u.id          AS userId,
       u.userName,
       d.amount      AS donationTotal,
       u.titleIdList AS titleIdListBefore
FROM user u
         INNER JOIN donation_records d ON d.userId = u.id
WHERE d.amount >= 29.90
  AND d.isDelete = 0
  AND u.isDelete = 0
  AND (
    u.titleIdList IS NULL
        OR TRIM(u.titleIdList) = ''
        OR TRIM(u.titleIdList) = '[]'
        OR NOT JSON_VALID(u.titleIdList)
        OR NOT JSON_CONTAINS(CAST(u.titleIdList AS JSON), '"31"', '$')
    )
ORDER BY d.amount DESC;

-- 1b. 预览：即将写入的系统通知（与上表用户一致；已发过 sponsor_title_31 的会跳过）
SELECT u.id     AS recipientId,
       u.userName,
       d.amount AS donationTotal,
       CONCAT(
               '感谢您累计赞助摸鱼岛 ',
               FORMAT(d.amount, 2),
               ' 元，已为您补发永久会员专属称号「闪耀永恒岛民」，感谢支持！'
       )        AS sourceContentPreview
FROM user u
         INNER JOIN donation_records d ON d.userId = u.id
WHERE d.amount >= 29.90
  AND d.isDelete = 0
  AND u.isDelete = 0
  AND (
    u.titleIdList IS NULL
        OR TRIM(u.titleIdList) = ''
        OR TRIM(u.titleIdList) = '[]'
        OR NOT JSON_VALID(u.titleIdList)
        OR NOT JSON_CONTAINS(CAST(u.titleIdList AS JSON), '"31"', '$')
    )
  AND NOT EXISTS (SELECT 1
                  FROM event_remind er
                  WHERE er.recipientId = u.id
                    AND er.action = 'sponsor_title_31'
                    AND er.senderId = -1
                    AND er.sourceId = -1
                    AND er.sourceType = 0
                    AND er.isDelete = 0)
ORDER BY d.amount DESC;

-- 2. 批量补发（建议事务执行：先通知，再改称号；确认后 COMMIT）
-- START TRANSACTION;

-- 2a. 写入 event_remind 系统通知（须在 UPDATE 称号前执行，条件与步骤 1 相同）
INSERT INTO event_remind (action, sourceId, sourceType, sourceContent, url, state, senderId, recipientId,
                          remindTime, isDelete)
SELECT 'sponsor_title_31',
       -1,
       0,
       CONCAT(
               '感谢您累计赞助摸鱼岛 ',
               FORMAT(d.amount, 2),
               ' 元，已为您补发永久会员专属称号「闪耀永恒岛民」，感谢支持！'
       ),
       '',
       0,
       -1,
       u.id,
       NOW(),
       0
FROM user u
         INNER JOIN donation_records d ON d.userId = u.id
WHERE d.amount >= 29.90
  AND d.isDelete = 0
  AND u.isDelete = 0
  AND (
    u.titleIdList IS NULL
        OR TRIM(u.titleIdList) = ''
        OR TRIM(u.titleIdList) = '[]'
        OR NOT JSON_VALID(u.titleIdList)
        OR NOT JSON_CONTAINS(CAST(u.titleIdList AS JSON), '"31"', '$')
    )
  AND NOT EXISTS (SELECT 1
                  FROM event_remind er
                  WHERE er.recipientId = u.id
                    AND er.action = 'sponsor_title_31'
                    AND er.senderId = -1
                    AND er.sourceId = -1
                    AND er.sourceType = 0
                    AND er.isDelete = 0);

-- 2b. 补发称号 31 到 user.titleIdList
UPDATE user u
    INNER JOIN donation_records d ON d.userId = u.id
SET u.titleIdList = CASE
                        WHEN u.titleIdList IS NULL
                            OR TRIM(u.titleIdList) = ''
                            OR TRIM(u.titleIdList) = '[]'
                            OR NOT JSON_VALID(u.titleIdList)
                            THEN '["31"]'
                        ELSE JSON_UNQUOTE(JSON_ARRAY_APPEND(CAST(u.titleIdList AS JSON), '$', '31'))
    END
WHERE d.amount >= 29.90
  AND d.isDelete = 0
  AND u.isDelete = 0
  AND (
    u.titleIdList IS NULL
        OR TRIM(u.titleIdList) = ''
        OR TRIM(u.titleIdList) = '[]'
        OR NOT JSON_VALID(u.titleIdList)
        OR NOT JSON_CONTAINS(CAST(u.titleIdList AS JSON), '"31"', '$')
    );

-- COMMIT;
-- ROLLBACK;

-- 3. 校验：累计赞助达标且仍缺称号 31 的用户应为 0 行
SELECT u.id AS userId, u.userName, d.amount AS donationTotal, u.titleIdList
FROM user u
         INNER JOIN donation_records d ON d.userId = u.id
WHERE d.amount >= 29.90
  AND d.isDelete = 0
  AND u.isDelete = 0
  AND (
    u.titleIdList IS NULL
        OR TRIM(u.titleIdList) = ''
        OR TRIM(u.titleIdList) = '[]'
        OR NOT JSON_VALID(u.titleIdList)
        OR NOT JSON_CONTAINS(CAST(u.titleIdList AS JSON), '"31"', '$')
    );

-- 4. 校验：已补发称号抽样（应包含 "31"）
SELECT u.id AS userId, u.userName, d.amount AS donationTotal, u.titleIdList
FROM user u
         INNER JOIN donation_records d ON d.userId = u.id
WHERE d.amount >= 29.90
  AND d.isDelete = 0
  AND u.isDelete = 0
  AND JSON_VALID(u.titleIdList)
  AND JSON_CONTAINS(CAST(u.titleIdList AS JSON), '"31"', '$')
ORDER BY d.amount DESC
LIMIT 20;

-- 5. 校验：达标用户均应有 sponsor_title_31 通知（应为 0 行）
SELECT u.id AS userId, u.userName, d.amount AS donationTotal
FROM user u
         INNER JOIN donation_records d ON d.userId = u.id
WHERE d.amount >= 29.90
  AND d.isDelete = 0
  AND u.isDelete = 0
  AND NOT EXISTS (SELECT 1
                  FROM event_remind er
                  WHERE er.recipientId = u.id
                    AND er.action = 'sponsor_title_31'
                    AND er.senderId = -1
                    AND er.sourceId = -1
                    AND er.sourceType = 0
                    AND er.isDelete = 0);

-- =============================================================================
-- 可选：已有称号 31、但当时未写入通知的用户（单独补通知，不改称号）
-- =============================================================================
-- INSERT INTO event_remind (action, sourceId, sourceType, sourceContent, url, state, senderId, recipientId,
--                           remindTime, isDelete)
-- SELECT 'sponsor_title_31',
--        -1,
--        0,
--        CONCAT(
--                '恭喜获得永久会员专属称号「闪耀永恒岛民」！',
--                '（您累计赞助 ',
--                FORMAT(d.amount, 2),
--                ' 元）'
--        ),
--        '',
--        0,
--        -1,
--        u.id,
--        NOW(),
--        0
-- FROM user u
--          INNER JOIN donation_records d ON d.userId = u.id
-- WHERE d.amount >= 29.90
--   AND d.isDelete = 0
--   AND u.isDelete = 0
--   AND JSON_VALID(u.titleIdList)
--   AND JSON_CONTAINS(CAST(u.titleIdList AS JSON), '"31"', '$')
--   AND NOT EXISTS (SELECT 1
--                   FROM event_remind er
--                   WHERE er.recipientId = u.id
--                     AND er.action = 'sponsor_title_31'
--                     AND er.senderId = -1
--                     AND er.sourceId = -1
--                     AND er.sourceType = 0
--                     AND er.isDelete = 0);
