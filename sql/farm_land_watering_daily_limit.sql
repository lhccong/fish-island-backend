-- Run once after farm_land_watering.sql for existing databases.
ALTER TABLE `farm_land`
    ADD COLUMN `dailyWaterCount` int NOT NULL DEFAULT 0 COMMENT '上次浇水所在日期的浇水次数'
    AFTER `lastWateredTime`;
