-- 为装备模板表新增 baseSpeed 字段
-- baseSpeed：基础速度，决定战斗先手，速度高的一方先攻击
ALTER TABLE item_templates
    ADD COLUMN baseSpeed INT DEFAULT NULL COMMENT '基础速度（决定战斗先手，速度高的一方先攻击）'
        AFTER baseHp;
