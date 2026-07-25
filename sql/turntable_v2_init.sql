-- 转盘2初始化：项链 / 翅膀 / 护手 配饰装备转盘
-- quality 映射：rarity=1→N(1), rarity=2→R(2), rarity=3→SR(3), rarity=4→SSR(4)
-- 概率权重总计 1000：N=500, R=300, SR=150, SSR=50（各稀有度内部均摊）

-- 创建转盘（ID=2，与转盘1配置一致：每次1积分，300次大保底）
INSERT INTO turntable (id, type, name, costPoints, guaranteeCount, status)
VALUES (2, 1, '配饰装备转盘', 1, 300, 1);

-- rarity=1 装备：蓝色吊坠、牧师袍（共2件，各占 500/2=250）
INSERT INTO turntable_prize (turntableId, prizeId, quality, prizeType, probability, stock)
SELECT 2,
       id,
       1,
       1,
       350,
       -1
FROM item_templates
WHERE name IN ('蓝色吊坠', '牧师袍')
  AND isDelete = 0;

-- rarity=2 装备：美丽之袍（共1件，占 300）
INSERT INTO turntable_prize (turntableId, prizeId, quality, prizeType, probability, stock)
SELECT 2,
       id,
       2,
       1,
       300,
       -1
FROM item_templates
WHERE name IN ('美丽之袍')
  AND isDelete = 0;

-- rarity=3 装备：龙之坠、机能护手、一级护手（共3件，各占 150/3=50）
INSERT INTO turntable_prize (turntableId, prizeId, quality, prizeType, probability, stock)
SELECT 2,
       id,
       3,
       1,
       50,
       -1
FROM item_templates
WHERE name IN ('龙之坠', '机能护手', '一级护手')
  AND isDelete = 0;

-- rarity=4 装备：炎之坠、烈焰披风、花之翼（共3件，各占 50/3≈17/17/16）
INSERT INTO turntable_prize (turntableId, prizeId, quality, prizeType, probability, stock)
SELECT 2,
       id,
       4,
       1,
       CASE name
           WHEN '炎之坠' THEN 7
           WHEN '烈焰披风' THEN 7
           WHEN '花之翼' THEN 6
       END,
       -1
FROM item_templates
WHERE name IN ('炎之坠', '烈焰披风', '花之翼')
  AND isDelete = 0;
