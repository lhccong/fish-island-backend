-- 农场作物解锁等级
ALTER TABLE `farm_crop`
    ADD COLUMN `unlockLevel` int NOT NULL DEFAULT 1 COMMENT '解锁所需农场等级' AFTER `rarity`;

-- 按稀有度初始化解锁等级：1级/3级/5级/8级
UPDATE `farm_crop` SET `unlockLevel` = 1 WHERE `rarity` = 1;
UPDATE `farm_crop` SET `unlockLevel` = 3 WHERE `rarity` = 2;
UPDATE `farm_crop` SET `unlockLevel` = 5 WHERE `rarity` = 3;
UPDATE `farm_crop` SET `unlockLevel` = 8 WHERE `rarity` = 4;
