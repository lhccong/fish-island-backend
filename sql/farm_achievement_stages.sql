-- 道具升级成就改为阶段性任务：每道具 × Lv2-Lv5，每档独立完成/领取
-- 奖励与升级即时奖励梯度对齐：100 / 200 / 350 / 500

-- 1. 清理旧的 buff 升级任务及其进度
DELETE FROM farm_achievement_progress WHERE taskId IN (
    SELECT id FROM farm_achievement_task WHERE type = 'buff_upgrade');
DELETE FROM farm_achievement_task WHERE type = 'buff_upgrade';

-- 2. 插入 4 道具 × 4 阶段
INSERT INTO farm_achievement_task (name, description, type, targetId, targetCount, rewardType, rewardValue, status, sortOrder)
VALUES
    ('加速起步',   '将【生长加速光环】升到 Lv2', 'buff_upgrade', '1', 2, 1, 100, 1, 1),
    ('风驰电掣',   '将【生长加速光环】升到 Lv3', 'buff_upgrade', '1', 3, 1, 200, 1, 2),
    ('争分夺秒',   '将【生长加速光环】升到 Lv4', 'buff_upgrade', '1', 4, 1, 350, 1, 3),
    ('光阴似箭',   '将【生长加速光环】升到 Lv5', 'buff_upgrade', '1', 5, 1, 500, 1, 4),
    ('丰收起步',   '将【丰收光环】升到 Lv2',     'buff_upgrade', '2', 2, 1, 100, 1, 5),
    ('仓廪渐实',   '将【丰收光环】升到 Lv3',     'buff_upgrade', '2', 3, 1, 200, 1, 6),
    ('硕果盈枝',   '将【丰收光环】升到 Lv4',     'buff_upgrade', '2', 4, 1, 350, 1, 7),
    ('五谷丰登',   '将【丰收光环】升到 Lv5',     'buff_upgrade', '2', 5, 1, 500, 1, 8),
    ('筑起篱笆',   '将【守护光环】升到 Lv2',     'buff_upgrade', '3', 2, 1, 100, 1, 9),
    ('固若金汤',   '将【守护光环】升到 Lv3',     'buff_upgrade', '3', 3, 1, 200, 1, 10),
    ('铜墙铁壁',   '将【守护光环】升到 Lv4',     'buff_upgrade', '3', 4, 1, 350, 1, 11),
    ('固守金城',   '将【守护光环】升到 Lv5',     'buff_upgrade', '3', 5, 1, 500, 1, 12),
    ('幸运初现',   '将【神奇生长光环】升到 Lv2', 'buff_upgrade', '4', 2, 1, 100, 1, 13),
    ('福星高照',   '将【神奇生长光环】升到 Lv3', 'buff_upgrade', '4', 3, 1, 200, 1, 14),
    ('鸿运当头',   '将【神奇生长光环】升到 Lv4', 'buff_upgrade', '4', 4, 1, 350, 1, 15),
    ('天选之人',   '将【神奇生长光环】升到 Lv5', 'buff_upgrade', '4', 5, 1, 500, 1, 16);

-- 3. 清理孤儿进度（任务已删除的）
DELETE FROM farm_achievement_progress WHERE taskId NOT IN (
    SELECT id FROM farm_achievement_task);

-- 4. 验证
SELECT id, name, targetId, targetCount, rewardValue FROM farm_achievement_task
WHERE type = 'buff_upgrade' ORDER BY sortOrder;
