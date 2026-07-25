-- 项链 / 翅膀 / 护手 转盘装备数值配置
-- 只更新数值字段：equip_slot, sub_type, rarity, levelReq, baseAttack, baseDefense, baseHp, mainAttr, removePoint
-- 数值梯度参考 equipment_value_v1.sql，项链主属性偏暴击，翅膀主属性偏连击，护手延续手套风格

-- start 版本 2 --------------------

-- ==================== 项链（equip_slot=necklace） ====================

-- 攻击:0 | 防御:10 | 生命:70 | 暴击:1.5%
-- 蓝色吊坠（rarity=1 项链）
UPDATE item_templates SET equip_slot = 'necklace', sub_type = 'necklace',
    rarity = 1, levelReq = 1, baseAttack = 0, baseDefense = 10, baseHp = 70,
    mainAttr = '"{\\"critRate\\":0.015}"', removePoint = 10
WHERE name = '蓝色吊坠';

-- 攻击:20 | 防御:28 | 生命:220 | 暴击:4% 抗暴击:2%
-- 龙之坠（rarity=3 项链）
UPDATE item_templates SET equip_slot = 'necklace', sub_type = 'necklace',
    rarity = 3, levelReq = 1, baseAttack = 20, baseDefense = 28, baseHp = 220,
    mainAttr = '"{\\"critRate\\":0.04,\\"critResistance\\":0.02}"', removePoint = 100
WHERE name = '龙之坠';

-- 攻击:45 | 防御:45 | 生命:420 | 暴击:6.5% 吸血:4% 抗暴击:4% 抗闪避:4%
-- 炎之坠（rarity=4 项链）
UPDATE item_templates SET equip_slot = 'necklace', sub_type = 'necklace',
    rarity = 4, levelReq = 1, baseAttack = 45, baseDefense = 45, baseHp = 420,
    mainAttr = '"{\\"critRate\\":0.065,\\"lifesteal\\":0.04,\\"critResistance\\":0.04,\\"dodgeResistance\\":0.04}"',
    removePoint = 600
WHERE name = '炎之坠';

-- ==================== 翅膀（equip_slot=wing） ====================

-- 攻击:0 | 防御:12 | 生命:65 | 连击:1.5%
-- 牧师袍（rarity=1 翅膀）
UPDATE item_templates SET equip_slot = 'wing', sub_type = 'wing',
    rarity = 1, levelReq = 1, baseAttack = 0, baseDefense = 12, baseHp = 65,
    mainAttr = '"{\\"comboRate\\":0.015}"', removePoint = 10
WHERE name = '牧师袍';

-- 攻击:10 | 防御:22 | 生命:140 | 连击:2.5% 闪避:1.5%
-- 美丽之袍（rarity=2 翅膀）
UPDATE item_templates SET equip_slot = 'wing', sub_type = 'wing',
    rarity = 2, levelReq = 1, baseAttack = 10, baseDefense = 22, baseHp = 140,
    mainAttr = '"{\\"comboRate\\":0.025,\\"dodgeRate\\":0.015}"', removePoint = 20
WHERE name = '美丽之袍';

-- 攻击:55 | 防御:75 | 生命:480 | 连击:6.5% 闪避:4% 抗连击:5% 抗闪避:4%
-- 烈焰披风（rarity=4 翅膀）
UPDATE item_templates SET equip_slot = 'wing', sub_type = 'wing',
    rarity = 4, levelReq = 1, baseAttack = 55, baseDefense = 75, baseHp = 480,
    mainAttr = '"{\\"comboRate\\":0.065,\\"dodgeRate\\":0.04,\\"comboResistance\\":0.05,\\"dodgeResistance\\":0.04}"',
    removePoint = 600
WHERE name = '烈焰披风';

-- 攻击:70 | 防御:45 | 生命:380 | 连击:6.5% 暴击:4% 吸血:3.5% 抗连击:4%
-- 玫瑰之翼（rarity=4 翅膀）
UPDATE item_templates SET equip_slot = 'wing', sub_type = 'wing',
    rarity = 4, levelReq = 1, baseAttack = 70, baseDefense = 45, baseHp = 380,
    mainAttr = '"{\\"comboRate\\":0.065,\\"critRate\\":0.04,\\"lifesteal\\":0.035,\\"comboResistance\\":0.04}"',
    removePoint = 600
WHERE name = '花之翼';

-- ==================== 护手（equip_slot=hand） ====================

-- 攻击:38 | 防御:42 | 生命:260 | 连击:4%
-- 机能护手（rarity=3 手套）
UPDATE item_templates SET equip_slot = 'hand', sub_type = 'hand',
    rarity = 3, levelReq = 1, baseAttack = 38, baseDefense = 42, baseHp = 260,
    mainAttr = '"{\\"comboRate\\":0.04}"', removePoint = 100
WHERE name = '机能护手';

-- 攻击:32 | 防御:50 | 生命:240 | 连击:3.5% 格挡:2%
-- 一级护手（rarity=3 手套）
UPDATE item_templates SET equip_slot = 'hand', sub_type = 'hand',
    rarity = 3, levelReq = 1, baseAttack = 32, baseDefense = 50, baseHp = 240,
    mainAttr = '"{\\"comboRate\\":0.035,\\"blockRate\\":0.02}"', removePoint = 100
WHERE name = '一级护手';

-- end 版本 2 --------------------
