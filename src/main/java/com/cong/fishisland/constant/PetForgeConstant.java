package com.cong.fishisland.constant;

/**
 * 宠物装备锻造常量
 *
 * <p>装备升级加成采用非线性公式，每级加成 = BASE * level^SCALE，
 * 高等级时成长更快，满级（20级）加成约为低等级的数倍。
 *
 * <p>各部位主属性：
 * <ul>
 *   <li>武器（1）  → 攻击力</li>
 *   <li>手套（2）  → 防御力</li>
 *   <li>鞋子（3）  → 速度</li>
 *   <li>头盔（4）  → 最大生命值</li>
 *   <li>项链（5）  → 暴击率</li>
 *   <li>翅膀（6）  → 连击率</li>
 * </ul>
 *
 * @author cong
 */
public interface PetForgeConstant {

    // ==================== 非线性成长指数 ====================

    /**
     * 装备等级加成成长指数
     * 每级加成 = BASE * level^SCALE
     * SCALE=1.5 时：1级=1x，5级≈11x，10级≈31x，20级≈89x
     */
    double LEVEL_SCALE = 1.5;

    // ==================== 武器：攻击力 ====================

    /**
     * 武器每级攻击力基础系数
     * 实际加成 = WEAPON_ATK_BASE * level^1.5
     * 1级=+3, 10级=+95, 20级=+268（满级约等于1.7条金色攻击词条上限）
     */
    int WEAPON_ATK_BASE = 3;

    // ==================== 手套：防御力 ====================

    /**
     * 手套每级防御力基础系数
     * 实际加成 = GLOVES_DEF_BASE * level^1.5
     * 1级=+2, 10级=+63, 20级=+179（满级约等于1.4条金色防御词条上限）
     */
    int GLOVES_DEF_BASE = 2;

    // ==================== 鞋子：速度 ====================

    /**
     * 鞋子每级速度基础系数
     * 实际加成 = SHOES_SPEED_BASE * level^1.5
     * 1级=+1, 10级=+31, 20级=+89（满级约等于较高速度加成）
     */
    int SHOES_SPEED_BASE = 1;

    // ==================== 头盔：最大生命值 ====================

    /**
     * 头盔每级生命值基础系数
     * 实际加成 = HELMET_HP_BASE * level^1.5
     * 1级=+15, 10级=+474, 20级=+1342（满级约等于2条金色生命词条上限）
     */
    int HELMET_HP_BASE = 15;

    // ==================== 项链：暴击率 ====================

    /**
     * 项链每级暴击率基础系数（单位：%，存库时除以 100 转为小数）
     * 实际加成 = NECKLACE_CRIT_BASE * level^1.5 / 100
     * 1级=+0.0015, 10级=+0.0474, 20级=+0.1342（满级约等于1.7条金色暴击词条上限）
     */
    double NECKLACE_CRIT_BASE = 0.15;

    // ==================== 翅膀：连击率 ====================

    /**
     * 翅膀每级连击率基础系数（单位：%，存库时除以 100 转为小数）
     * 实际加成 = WINGS_COMBO_BASE * level^1.5 / 100
     * 1级=+0.0015, 10级=+0.0474, 20级=+0.1342（满级约等于1.7条金色连击词条上限）
     */
    double WINGS_COMBO_BASE = 0.15;

    // ==================== 兼容旧字段（已废弃，勿新增使用） ====================

    /** @deprecated 已改为按部位差异化加成，请使用 WEAPON_ATK_BASE */
    @Deprecated
    int LEVEL_BONUS_ATTACK = 8;

    /** @deprecated 已改为按部位差异化加成，请使用 GLOVES_DEF_BASE */
    @Deprecated
    int LEVEL_BONUS_DEFENSE = 5;

    /** @deprecated 已改为按部位差异化加成，请使用 HELMET_HP_BASE */
    @Deprecated
    int LEVEL_BONUS_HP = 30;

    /** @deprecated 已改为按部位差异化加成，请使用对应部位常量 */
    @Deprecated
    double LEVEL_BONUS_PCT = 0.3;
}
