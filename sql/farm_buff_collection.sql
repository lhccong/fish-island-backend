-- 农场 buff 道具 + 作物图鉴系统（PRD v2.1）
-- 包含：3 张新表、farm_user/farm_crop 字段变更、成就任务与示例作物配置数据

-- ----------------------------
-- 1. 农场 buff 道具持有表
-- ----------------------------
CREATE TABLE IF NOT EXISTS farm_user_buff
(
    id         BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    userId     BIGINT   NOT NULL COMMENT '用户ID',
    buffType   TINYINT  NOT NULL COMMENT '道具类型：1-生长加速 2-积分增产 3-守护 4-神奇生长',
    level      TINYINT  DEFAULT 1 NOT NULL COMMENT '当前等级，1 起',
    unlockTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '首次购买时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_buff (userId, buffType)
) COMMENT '农场 buff 道具持有表' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 2. 作物图鉴表（品级 × 最大重量，同作物同品级仅一条）
-- ----------------------------
CREATE TABLE IF NOT EXISTS farm_collection_grade
(
    id                BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    userId            BIGINT  NOT NULL COMMENT '用户ID',
    cropId            BIGINT  NOT NULL COMMENT '作物ID',
    grade             TINYINT NOT NULL COMMENT '图鉴品级 1-10（与种子稀有度 rarity 无关）',
    maxWeight         INT     NOT NULL COMMENT '该品级已记录的最大重量（克）',
    firstObtainedTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '首次解锁该品级时间',
    createTime        DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_crop_grade (userId, cropId, grade),
    INDEX idx_user (userId)
) COMMENT '作物图鉴表' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 3. 农场成就任务配置表
-- ----------------------------
CREATE TABLE IF NOT EXISTS farm_achievement_task
(
    id          BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL COMMENT '成就名称',
    description VARCHAR(200) NULL COMMENT '成就描述',
    type        VARCHAR(20)  NOT NULL COMMENT '类型：buff_upgrade/buff_purchase/collection_unlock/farm_level',
    targetId    VARCHAR(64)  NULL COMMENT '目标对象（buffType / cropId / grade 等）',
    targetCount INT          NOT NULL COMMENT '目标值（等级/条目数/品级）',
    rewardType  TINYINT      DEFAULT 1 NOT NULL COMMENT '奖励类型：1-积分 2-稀有种子',
    rewardValue INT          NOT NULL COMMENT '奖励数值',
    status      TINYINT      DEFAULT 1 NOT NULL COMMENT '1-启用 0-禁用',
    sortOrder   INT          DEFAULT 0 NOT NULL,
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '农场成就任务配置表' COLLATE = utf8mb4_unicode_ci;

-- 成就任务完成/领取进度表
CREATE TABLE IF NOT EXISTS farm_achievement_progress
(
    id            BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    userId        BIGINT  NOT NULL COMMENT '用户ID',
    taskId        BIGINT  NOT NULL COMMENT '成就任务ID',
    status        TINYINT DEFAULT 0 NOT NULL COMMENT '0-进行中 1-已完成未领取 2-已领取',
    completedTime DATETIME NULL COMMENT '达成时间',
    claimedTime   DATETIME NULL COMMENT '领取时间',
    createTime    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_task (userId, taskId)
) COMMENT '农场成就任务进度表' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 4. 存量表字段变更
-- ----------------------------
ALTER TABLE farm_user
    ADD COLUMN lastLevelRewardLevel INT DEFAULT 0 NOT NULL COMMENT '已领取等级奖励的最高档位（每5级一档）';

ALTER TABLE farm_crop
    ADD COLUMN baseWeight INT NOT NULL DEFAULT 100 COMMENT '初始重量（克），基准按品类设定',
    ADD COLUMN baseGrade TINYINT NOT NULL DEFAULT 1 COMMENT '初始品级（图鉴收集起点）',
    ADD COLUMN gradeWeightJson VARCHAR(512) NULL COMMENT '10 品级概率权重 JSON（缺省用全局默认）',
    ADD COLUMN collectionIconsJson VARCHAR(1024) NULL COMMENT '10 品级图鉴图标路径 JSON（缺省用作物图标）';

-- ----------------------------
-- 5. 作物种子配置（对齐正式环境风格：个位数前期种植消耗；生长时间 4h~48h；
--    解锁门槛 1~20 级；baseWeight 为符合实际的基准重量）
-- ----------------------------
DELETE FROM farm_crop;
INSERT INTO farm_crop (id, name, category, growthTime, experience, coin, price, rarity, unlockLevel, icon, description, baseWeight, baseGrade)
VALUES
    (1, '小草苗', '粮食', 240, 5,  3,  1, 1, 1,  '🌱', '嫩绿的小苗，勤劳的开端', 50, 1),
    (2, '小番茄', '蔬菜', 360, 6,  5,  2, 1, 1,  '🍅', '红彤彤的番茄，需要耐心等待', 80, 1),
    (3, '西兰花', '蔬菜', 480, 8,  7,  2, 1, 2,  '🥦', '营养丰富的绿色花球', 400, 1),
    (4, '小南瓜', '水果', 600, 10, 10, 3, 1, 3,  '🎃', '沉甸甸的南瓜，半天就能成熟', 2500, 1),
    (5,  '玉米',   '粮食', 720, 13, 14, 4, 1, 5,  '🌽', '金黄饱满的玉米棒', 200, 1),
    (6,  '茄子',   '蔬菜', 900, 16, 18, 5, 1, 7,  '🍆', '紫得发亮的细长茄子', 250, 1),
    (7,  '葡萄',   '水果', 1080, 20, 23, 7, 2, 9,  '🍇', '一串晶莹剔透的葡萄', 600, 1),
    (13, '草莓',   '水果', 1200, 23, 26, 8, 2, 10, '🍓', '鲜红多汁的草莓，甜过初恋', 700, 1),
    (8,  '向日葵', '花卉', 1440, 26, 30, 9, 2, 12, '🌻', '永远面向太阳的花盘', 800, 1),
    (9,  '西瓜',   '水果', 1800, 34, 40, 12, 3, 15, '🍉', '沙瓤大西瓜，一天半养成', 5000, 1),
    (10, '蝴蝶兰', '花卉', 2880, 50, 60, 18, 3, 20, '🌸', '两天心血浇灌的高贵兰花', 1200, 1);

-- ----------------------------
-- 6. 成就任务配置（buff 升级为阶段性任务：每道具 Lv2-Lv5 各一档）
-- ----------------------------
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
    ('守护启程',   '解锁并购买【守护光环】',     'buff_purchase', '3', 1, 1, 150, 1, 9),
    ('筑起篱笆',   '将【守护光环】升到 Lv2',     'buff_upgrade', '3', 2, 1, 100, 1, 10),
    ('固若金汤',   '将【守护光环】升到 Lv3',     'buff_upgrade', '3', 3, 1, 200, 1, 11),
    ('铜墙铁壁',   '将【守护光环】升到 Lv4',     'buff_upgrade', '3', 4, 1, 350, 1, 12),
    ('固守金城',   '将【守护光环】升到 Lv5',     'buff_upgrade', '3', 5, 1, 500, 1, 13),
    ('幸运初现',   '将【神奇生长光环】升到 Lv2', 'buff_upgrade', '4', 2, 1, 100, 1, 14),
    ('福星高照',   '将【神奇生长光环】升到 Lv3', 'buff_upgrade', '4', 3, 1, 200, 1, 15),
    ('鸿运当头',   '将【神奇生长光环】升到 Lv4', 'buff_upgrade', '4', 4, 1, 350, 1, 16),
    ('天选之人',   '将【神奇生长光环】升到 Lv5', 'buff_upgrade', '4', 5, 1, 500, 1, 17),
    ('传说品质',   '解锁任意品级 10 的作物图鉴', 'collection_unlock', NULL, 10, 1, 500, 1, 18),
    ('农场大亨',   '农场等级达到 25 级',         'farm_level', NULL, 25, 1, 300, 1, 19);
