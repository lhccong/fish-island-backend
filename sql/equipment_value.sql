-- 只更新数值字段：rarity, levelReq, baseAttack, baseDefense, baseHp, mainAttr, removePoint

-- 攻击:15 | 防御:0 | 生命:0 | 暴击:1.5% 连击:1.5%
-- 普通的钥匙（rarity=1 武器）
UPDATE fish.item_templates SET rarity = 1, levelReq = 1, baseAttack = 15, baseDefense = 0, baseHp = 0,
    mainAttr = '"{\\"critRate\\":0.015,\\"comboRate\\":0.015}"', removePoint = 10
WHERE id = 2004531769355710465;

-- 攻击:0 | 防御:15 | 生命:80 | 闪避:1.5%
-- 勇者围脖（rarity=1 头盔）
UPDATE fish.item_templates SET rarity = 1, levelReq = 1, baseAttack = 0, baseDefense = 15, baseHp = 80,
    mainAttr = '"{\\"dodgeRate\\":0.015}"', removePoint = 10
WHERE id = 2004531802280996866;

-- 攻击:0 | 防御:40 | 生命:0 | 闪避:2.5%
-- 蔚蓝摸鱼帽（rarity=2 头盔）
UPDATE fish.item_templates SET rarity = 2, levelReq = 1, baseAttack = 0, baseDefense = 40, baseHp = 0,
    mainAttr = '"{\\"dodgeRate\\":0.025}"', removePoint = 20
WHERE id = 2004531793728811009;

-- 攻击:25 | 防御:25 | 生命:180 | 连击:2.5%
-- 纯白护手（rarity=2 手套）
UPDATE fish.item_templates SET rarity = 2, levelReq = 1, baseAttack = 25, baseDefense = 25, baseHp = 180,
    mainAttr = '"{\\"comboRate\\":0.025}"', removePoint = 20
WHERE id = 2004531897873379329;

-- 攻击:30 | 防御:20 | 生命:120 | 连击:2.5%
-- 紫色护手（rarity=2 手套）
UPDATE fish.item_templates SET rarity = 2, levelReq = 1, baseAttack = 30, baseDefense = 20, baseHp = 120,
    mainAttr = '"{\\"comboRate\\":0.025}"', removePoint = 20
WHERE id = 2004531891992965121;

-- 攻击:20 | 防御:65 | 生命:300 | 闪避:4%
-- 纯白之裙（rarity=3 鞋子）
UPDATE fish.item_templates SET rarity = 3, levelReq = 1, baseAttack = 20, baseDefense = 65, baseHp = 300,
    mainAttr = '"{\\"dodgeRate\\":0.04}"', removePoint = 100
WHERE id = 2004531899047784449;

-- 攻击:50 | 防御:60 | 生命:350 | 闪避:4% 抗连击:4%
-- 紫色发冠（rarity=3 头盔）
UPDATE fish.item_templates SET rarity = 3, levelReq = 1, baseAttack = 50, baseDefense = 60, baseHp = 350,
    mainAttr = '"{\\"dodgeRate\\":0.04,\\"comboResistance\\":0.04}"', removePoint = 100
WHERE id = 2004531893171564545;

-- 攻击:80 | 防御:90 | 生命:550 | 格挡:6.5% 闪避:6.5% 连击:3.5% 抗连击:6.5% 抗暴击:6.5% 抗闪避:6.5% 抗格挡:6.5% 抗吸血:6.5%
-- 花之冠（rarity=4 头盔）
UPDATE fish.item_templates SET rarity = 4, levelReq = 1, baseAttack = 80, baseDefense = 90, baseHp = 550,
    mainAttr = '"{\\"blockRate\\":0.065,\\"dodgeRate\\":0.065,\\"comboRate\\":0.035,\\"comboResistance\\":0.065,\\"critResistance\\":0.065,\\"dodgeResistance\\":0.065,\\"blockResistance\\":0.065,\\"lifestealResistance\\":0.065}"',
    removePoint = 1000
WHERE id = 2004531795628830722;

-- 攻击:130 | 防御:0 | 生命:0 | 暴击:6.5% 连击:6.5% 吸血:4.5% 格挡:3.5% 闪避:3.5% 抗暴击:3.5% 抗闪避:6.5% 抗吸血:3.5% 抗连击:3.5% 抗格挡:3.5%
-- 审判引擎（rarity=4 武器）
UPDATE fish.item_templates SET rarity = 4, levelReq = 1, baseAttack = 130, baseDefense = 0, baseHp = 0,
    mainAttr = '"{\\"critRate\\":0.065,\\"comboRate\\":0.065,\\"lifesteal\\":0.045,\\"blockRate\\":0.035,\\"dodgeRate\\":0.035,\\"critResistance\\":0.035,\\"dodgeResistance\\":0.065,\\"lifestealResistance\\":0.035,\\"comboResistance\\":0.035,\\"blockResistance\\":0.035}"',
    removePoint = 1000
WHERE id = 2004531905314074626;
