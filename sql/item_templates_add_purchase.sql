-- item_templates 新增购买相关字段
ALTER TABLE `item_templates`
    ADD COLUMN `purchasable`   TINYINT  DEFAULT 0 NULL COMMENT '是否允许购买，0-不可购买，1-可购买' AFTER `removePoint`,
    ADD COLUMN `purchasePoint` INT      DEFAULT 0 NULL COMMENT '购买消耗积分，purchasable=1 时有效' AFTER `purchasable`;
