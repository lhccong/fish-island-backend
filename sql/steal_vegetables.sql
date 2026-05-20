-- fish.farm_collection definition

-- fish.farm_collection definition

CREATE TABLE `farm_collection`
(
    `id`           bigint NOT NULL AUTO_INCREMENT COMMENT '图鉴ID',
    `userId`       bigint NOT NULL COMMENT '用户ID',
    `cropId`       bigint NOT NULL COMMENT '作物ID',
    `obtained`     tinyint  DEFAULT '0' COMMENT '是否已获得',
    `obtainedTime` datetime DEFAULT NULL COMMENT '获得时间',
    `count`        int      DEFAULT '0' COMMENT '收集数量',
    `createdAt`    datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updatedAt`    datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_crop` (`userId`,`cropId`),
    KEY            `idx_user_id` (`userId`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='图鉴收集表';

-- fish.farm_crop definition

-- fish.farm_crop definition

CREATE TABLE `farm_crop`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '作物ID',
    `name`        varchar(50) NOT NULL COMMENT '作物名称',
    `category`    varchar(20) NOT NULL COMMENT '分类:粮食/蔬菜/水果/花卉',
    `growthTime`  int         NOT NULL COMMENT '成长时间(分钟)',
    `experience`  int         NOT NULL COMMENT '收获经验',
    `coin`        int         NOT NULL COMMENT '收获积分',
    `price`       int         NOT NULL DEFAULT '0' COMMENT '购买价格(积分)',
    `rarity`      tinyint              DEFAULT '1' COMMENT '稀有度:1-普通,2-稀有,3-史诗,4-传说',
    `icon`        varchar(255)         DEFAULT '' COMMENT '图标',
    `description` varchar(200)         DEFAULT '' COMMENT '描述',
    `createdAt`   datetime             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作物表';

-- fish.farm_daily_task definition

-- fish.farm_daily_task definition

CREATE TABLE `farm_daily_task`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `name`        varchar(50) NOT NULL COMMENT '任务名称',
    `description` varchar(200) DEFAULT '' COMMENT '任务描述',
    `targetCount` int         NOT NULL COMMENT '目标完成次数',
    `rewardExp`   int         NOT NULL COMMENT '奖励经验',
    `type`        varchar(20) NOT NULL COMMENT '任务类型:harvest/replant/visit/steal',
    `sortOrder`   int          DEFAULT '0' COMMENT '排序',
    `createdAt`   datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='每日任务表';

-- fish.farm_land definition


CREATE TABLE `farm_land`
(
    `id`            bigint NOT NULL AUTO_INCREMENT COMMENT '地块ID',
    `userId`        bigint NOT NULL COMMENT '用户ID',
    `landIndex`     int    NOT NULL COMMENT '地块序号(1-9)',
    `status`        tinyint  DEFAULT '0' COMMENT '状态:0-空地,1-种植中,2-可收获',
    `plantedCropId` bigint   DEFAULT NULL COMMENT '种植的作物ID',
    `plantedTime`   datetime DEFAULT NULL COMMENT '种植时间',
    `harvestTime`   datetime DEFAULT NULL COMMENT '可收获时间',
    `locked`        tinyint  DEFAULT '0' COMMENT '是否锁定',
    `createdAt`     datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updatedAt`     datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_land` (`userId`,`landIndex`),
    KEY             `idx_user_id` (`userId`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='地块表';

-- fish.farm_plant_record definition

CREATE TABLE `farm_plant_record`
(
    `id`                  bigint   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `userId`              bigint   NOT NULL COMMENT '用户ID',
    `landId`              bigint   NOT NULL COMMENT '地块ID',
    `cropId`              bigint   NOT NULL COMMENT '作物ID',
    `plantedTime`         datetime NOT NULL COMMENT '种植时间',
    `harvestTime`         datetime NOT NULL COMMENT '可收获时间',
    `harvested`           tinyint           DEFAULT '0' COMMENT '是否已收获',
    `harvestedTime`       datetime          DEFAULT NULL COMMENT '收获时间',
    `plantedPointsReward` int               DEFAULT NULL COMMENT '种植时预期积分奖励',
    `stolenPoints`        int      NOT NULL DEFAULT '0' COMMENT '被偷积分',
    `stolenCount`         int               DEFAULT '0' COMMENT '被偷次数',
    `createdAt`           datetime          DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY                   `idx_user_id` (`userId`),
    KEY                   `idx_harvest_time` (`harvestTime`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='种植记录表';

-- fish.farm_ranking definition

-- fish.farm_ranking definition

CREATE TABLE `farm_ranking`
(
    `id`         bigint      NOT NULL AUTO_INCREMENT COMMENT '排行ID',
    `userId`     bigint      NOT NULL COMMENT '用户ID',
    `type`       varchar(20) NOT NULL COMMENT '排行类型:steal_exp/steal_count/defense',
    `todayValue` int      DEFAULT '0' COMMENT '今日数值',
    `totalValue` int      DEFAULT '0' COMMENT '累计数值',
    `date`       date        NOT NULL COMMENT '日期',
    `createdAt`  datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updatedAt`  datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_type_date` (`userId`,`type`,`date`),
    KEY          `idx_type_date` (`type`,`date`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='排行榜表';

-- fish.farm_steal_record definition

CREATE TABLE `farm_steal_record`
(
    `id`            bigint NOT NULL AUTO_INCREMENT COMMENT '偷取记录ID',
    `stealerId`     bigint NOT NULL COMMENT '偷取者ID',
    `ownerId`       bigint NOT NULL COMMENT '被偷者ID',
    `plantRecordId` bigint NOT NULL COMMENT '种植记录ID',
    `cropId`        bigint NOT NULL COMMENT '作物ID',
    `stolenTime`    datetime DEFAULT CURRENT_TIMESTAMP COMMENT '偷取时间',
    `expGained`     int      DEFAULT '0' COMMENT '获得经验',
    `coinGained`    int      DEFAULT '0' COMMENT '获得积分',
    PRIMARY KEY (`id`),
    KEY             `idx_stealer_id` (`stealerId`),
    KEY             `idx_owner_id` (`ownerId`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='偷取记录表';

-- fish.farm_task_record definition

CREATE TABLE `farm_task_record`
(
    `id`           bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `userId`       bigint NOT NULL COMMENT '用户ID',
    `taskId`       bigint NOT NULL COMMENT '任务ID',
    `currentCount` int      DEFAULT '0' COMMENT '当前完成次数',
    `completed`    tinyint  DEFAULT '0' COMMENT '是否已完成',
    `claimed`      tinyint  DEFAULT '0' COMMENT '是否已领取奖励',
    `date`         date   NOT NULL COMMENT '日期',
    `createdAt`    datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updatedAt`    datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_task_date` (`userId`,`taskId`,`date`),
    KEY            `idx_user_id` (`userId`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务完成记录表';

-- fish.farm_user definition

CREATE TABLE `farm_user`
(
    `id`              bigint      NOT NULL AUTO_INCREMENT COMMENT '农场用户ID',
    `userId`          bigint      NOT NULL COMMENT '关联的系统用户ID',
    `nickname`        varchar(50) NOT NULL COMMENT '农场昵称',
    `avatar`          varchar(255) DEFAULT '' COMMENT '农场头像',
    `level`           int          DEFAULT '1' COMMENT '农场等级',
    `experience`      int          DEFAULT '0' COMMENT '经验值',
    `totalHarvest`    int          DEFAULT '0' COMMENT '总收获次数',
    `totalSteal`      int          DEFAULT '0' COMMENT '总偷菜次数',
    `totalDefense`    int          DEFAULT '0' COMMENT '总防御次数',
    `friendCount`     int          DEFAULT '0' COMMENT '好友数量',
    `visitedCount`    int          DEFAULT '0' COMMENT '被访问次数',
    `lastSignInDate`  datetime     DEFAULT NULL COMMENT '最后签到时间',
    `consecutiveDays` int          DEFAULT '0' COMMENT '连续签到天数',
    `status`          tinyint      DEFAULT '1' COMMENT '状态:0-封禁,1-正常',
    `createdAt`       datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updatedAt`       datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`userId`),
    KEY               `idx_level` (`level`),
    KEY               `idx_experience` (`experience`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农场用户表';

-- fish.farm_friend definition

CREATE TABLE `farm_friend`
(
    `id`            bigint NOT NULL AUTO_INCREMENT COMMENT '好友关系ID',
    `userId`        bigint NOT NULL COMMENT '用户ID（主人）',
    `friendId`      bigint NOT NULL COMMENT '好友用户ID',
    `status`        tinyint  DEFAULT '1' COMMENT '好友状态:0-拉黑,1-正常',
    `lastVisitTime` datetime DEFAULT NULL COMMENT '上次访问好友农场时间',
    `stealCooldown` datetime DEFAULT NULL COMMENT '偷菜冷却结束时间',
    `createdAt`     datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updatedAt`     datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_friend` (`userId`,`friendId`),
    KEY             `idx_user_id` (`userId`),
    KEY             `idx_friend_id` (`friendId`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农场好友表';

-- 蔬菜类作物
INSERT INTO farm_crop (name, category, growthTime, experience, coin, rarity, icon, description, createdAt)
VALUES ('胡萝卜', 'vegetable', 30, 7, 3, 1, '', '普通蔬菜', NOW()),
       ('卷心菜', 'vegetable', 45, 8, 4, 1, '', '普通蔬菜', NOW()),
       ('番茄', 'vegetable', 60, 10, 5, 2, '', '普通蔬菜', NOW()),
       ('黄瓜', 'vegetable', 75, 11, 5, 2, '', '普通蔬菜', NOW()),
       ('辣椒', 'vegetable', 90, 13, 6, 3, '', '普通蔬菜', NOW()),
       ('茄子', 'vegetable', 105, 14, 7, 3, '', '普通蔬菜', NOW()),
       ('豌豆', 'vegetable', 90, 13, 6, 3, '', '普通蔬菜', NOW()),
       ('芹菜', 'vegetable', 100, 14, 7, 3, '', '普通蔬菜', NOW()),
       ('土豆', 'vegetable', 120, 15, 7, 4, '', '普通蔬菜', NOW()),
       ('洋葱', 'vegetable', 150, 17, 8, 4, '', '普通蔬菜', NOW()),
       ('豆角', 'vegetable', 135, 16, 8, 4, '', '普通蔬菜', NOW());

-- 粮食类作物
INSERT INTO farm_crop (name, category, growthTime, experience, coin, rarity, icon, description, createdAt)
VALUES ('大麦', 'grain', 25, 6, 3, 1, '', '普通粮食作物', NOW()),
       ('燕麦', 'grain', 40, 8, 4, 1, '', '普通粮食作物', NOW()),
       ('黑麦', 'grain', 60, 10, 5, 2, '', '普通粮食作物', NOW()),
       ('高粱', 'grain', 90, 13, 6, 3, '', '普通粮食作物', NOW()),
       ('荞麦', 'grain', 120, 17, 8, 2, '', '优良品质粮食作物', NOW()),
       ('玉米', 'grain', 60, 10, 5, 3, '', '普通粮食作物', NOW());

-- 水果类作物
INSERT INTO farm_crop (name, category, growthTime, experience, coin, rarity, icon, description, createdAt)
VALUES ('草莓', 'fruit', 0, 0, 4, 1, '', '普通水果', NOW()),
       ('葡萄', 'fruit', 0, 0, 7, 1, '', '普通水果', NOW()),
       ('西瓜', 'fruit', 0, 0, 8, 1, '', '普通水果', NOW());

-- 花卉类作物
INSERT INTO farm_crop (name, category, growthTime, experience, coin, rarity, icon, description, createdAt)
VALUES ('向日葵', 'flower', 60, 10, 5, 1, '', '普通花卉', NOW()),
       ('郁金香', 'flower', 90, 13, 6, 2, '', '普通花卉', NOW()),
       ('雏菊', 'flower', 75, 11, 5, 2, '', '普通花卉', NOW()),
       ('康乃馨', 'flower', 120, 15, 7, 3, '', '普通花卉', NOW()),
       ('玫瑰', 'flower', 150, 20, 10, 2, '', '优良品质花卉', NOW());


INSERT INTO fish.farm_daily_task
(id, name, description, targetCount, rewardExp, `type`, sortOrder, createdAt)
VALUES (1, '种植作物', '种植3次作物', 3, 10, 'plant', 1, '2026-05-18 02:00:53');
INSERT INTO fish.farm_daily_task
(id, name, description, targetCount, rewardExp, `type`, sortOrder, createdAt)
VALUES (2, '收获作物', '收获3次作物', 3, 10, 'harvest', 2, '2026-05-18 02:00:53');
INSERT INTO fish.farm_daily_task
(id, name, description, targetCount, rewardExp, `type`, sortOrder, createdAt)
VALUES (3, '偷取好友作物', '偷取2次好友作物', 2, 15, 'steal', 3, '2026-05-18 02:00:53');
INSERT INTO fish.farm_daily_task
(id, name, description, targetCount, rewardExp, `type`, sortOrder, createdAt)
VALUES (4, '访问好友农场', '访问3个好友农场', 3, 5, 'visit', 4, '2026-05-18 02:00:53');
INSERT INTO fish.farm_daily_task
(id, name, description, targetCount, rewardExp, `type`, sortOrder, createdAt)
VALUES (5, '农场签到', '完成每日签到', 1, 5, 'signin', 5, '2026-05-18 02:00:53');