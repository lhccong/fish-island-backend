-- 宠物装备锻造表
-- 装备位置：1-武器 2-手套 3-鞋子 4-头盔 5-项链 6-翅膀
-- 词条等级：WHITE=白 BLUE=蓝 PURPLE=紫 GOLD=金 RED=红
-- 词条属性：ATTACK=攻击力 MAX_HP=最大生命值 DEFENSE=防御力
--           CRIT_RATE=暴击率 COMBO_RATE=连击率 DODGE_RATE=闪避率
--           BLOCK_RATE=格挡率 LIFESTEAL=吸血率
--           及对应抗性：ANTI_CRIT ANTI_COMBO ANTI_DODGE ANTI_BLOCK ANTI_LIFESTEAL

create table if not exists pet_equip_forge
(
    id          BIGINT       auto_increment comment '主键ID' PRIMARY KEY,
    petId       BIGINT       not null comment '宠物ID',
    equipSlot   TINYINT      not null comment '装备位置 1-武器 2-手套 3-鞋子 4-头盔 5-项链 6-翅膀',
    equipLevel  TINYINT      not null default 0 comment '装备等级（默认0）',
    entry1      JSON         default null comment '词条1 {attr: 词条属性, grade: 词条等级, value: 属性数值, locked: 是否锁定}',
    entry2      JSON         default null comment '词条2 {attr: 词条属性, grade: 词条等级, value: 属性数值, locked: 是否锁定}',
    entry3      JSON         default null comment '词条3 {attr: 词条属性, grade: 词条等级, value: 属性数值, locked: 是否锁定}',
    entry4      JSON         default null comment '词条4 {attr: 词条属性, grade: 词条等级, value: 属性数值, locked: 是否锁定}',
    createTime  DATETIME     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  DATETIME     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    TINYINT      default 0 not null comment '是否删除',
    UNIQUE KEY uk_pet_slot (petId, equipSlot)
) comment '宠物装备锻造表' collate = utf8mb4_unicode_ci;
