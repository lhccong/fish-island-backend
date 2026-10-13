-- Existing databases: run once before deploying the watering endpoint.
ALTER TABLE `farm_land`
    ADD COLUMN `lastWateredTime` datetime DEFAULT NULL COMMENT '本轮种植上次浇水时间'
    AFTER `harvestTime`;
