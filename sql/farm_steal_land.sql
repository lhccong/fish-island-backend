-- 偷菜记录增加地块ID，冷却与次数按地块维度统计
ALTER TABLE `farm_steal_record`
    ADD COLUMN `landId` bigint NULL COMMENT '地块ID' AFTER `ownerId`;

UPDATE `farm_steal_record` fsr
    INNER JOIN `farm_plant_record` fpr ON fsr.plantRecordId = fpr.id
SET fsr.landId = fpr.landId
WHERE fsr.landId IS NULL;

ALTER TABLE `farm_steal_record`
    ADD KEY `idx_stealer_land` (`stealerId`, `landId`);
