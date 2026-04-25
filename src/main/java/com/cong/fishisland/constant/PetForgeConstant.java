package com.cong.fishisland.constant;

/**
 * 宠物装备锻造常量
 *
 * @author cong
 */
public interface PetForgeConstant {

    /**
     * 每装备等级提供的攻击力加成
     * 满级20级 = +160，约等于1条金色词条上限（101~160）
     */
    int LEVEL_BONUS_ATTACK = 8;

    /**
     * 每装备等级提供的防御力加成
     * 满级20级 = +100，约等于0.8条金色词条上限（81~130）
     */
    int LEVEL_BONUS_DEFENSE = 5;

    /**
     * 每装备等级提供的生命值加成
     * 满级20级 = +600，约等于1条金色词条上限（421~650）
     */
    int LEVEL_BONUS_HP = 30;

    /**
     * 每装备等级提供的概率属性加成（单位：%）
     * 满级20级 = +6%，约等于0.75条金色词条上限（5.1~8.0%）
     */
    double LEVEL_BONUS_PCT = 0.3;
}
