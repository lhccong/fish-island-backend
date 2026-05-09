-- 转盘奖励初始化：根据装备稀有度写入 turntable_prize（转盘ID=1，宠物装备转盘）
-- quality 映射：rarity=1→N(1), rarity=2→R(2), rarity=3→SR(3), rarity=4→SSR(4)
-- 概率权重总计 1000：N=500, R=300, SR=150, SSR=50（各稀有度内部均摊）
--
-- rarity=1 装备：普通的法杖、勇者围脖（共2件，各占 500/2=250）
INSERT INTO turntable_prize (turntableId, prizeId, quality, prizeType, probability, stock)
SELECT 1,
       id,
       1,
       1,
       250,
       -1
FROM item_templates
WHERE name IN ('普通的法杖', '勇者围脖')
  AND isDelete = 0;

-- rarity=2 装备：野人帽、纯白护手、紫色护手（共3件，各占 300/3=100）
INSERT INTO turntable_prize (turntableId, prizeId, quality, prizeType, probability, stock)
SELECT 1,
       id,
       2,
       1,
       100,
       -1
FROM item_templates
WHERE name IN ('野人帽', '纯白护手', '紫色护手')
  AND isDelete = 0;

-- rarity=3 装备：烈焰靴、魔术帽（共2件，各占 150/2=75）
INSERT INTO turntable_prize (turntableId, prizeId, quality, prizeType, probability, stock)
SELECT 1,
       id,
       3,
       1,
       75,
       -1
FROM item_templates
WHERE name IN ('烈焰靴', '魔术帽')
  AND isDelete = 0;

-- rarity=4 装备：花之冠、审判引擎（共2件，各占 50/2=25）
INSERT INTO turntable_prize (turntableId, prizeId, quality, prizeType, probability, stock)
SELECT 1,
       id,
       4,
       1,
       25,
       -1
FROM item_templates
WHERE name IN ('花之冠', '审判引擎')
  AND isDelete = 0;
