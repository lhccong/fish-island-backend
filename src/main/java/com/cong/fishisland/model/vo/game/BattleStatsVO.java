package com.cong.fishisland.model.vo.game;

import lombok.Data;

/**
 * 战斗属性VO
 * <p>
 * 统一封装宠物或Boss在一场战斗中所需的全部属性，
 * 通过 {@link #fromPet} 或 {@link #fromBoss} 工厂方法构建，
 * 避免在各战斗服务中重复进行 null 安全的属性提取。
 *
 * @author cong
 */
@Data
public class BattleStatsVO {

    // ---- 基础属性 ----

    /** 攻击力 */
    private int attack;

    /** 最大生命值 */
    private int health;

    /** 防御力（每点防御减少1点伤害，最低造成1点） */
    private int defense;

    /** 速度（决定战斗先手，速度高的一方先攻击；相同时随机决定） */
    private int speed;

    // ---- 主动属性 ----

    /** 暴击率（0.0 ~ 1.0），触发后伤害 × {@code CRITICAL_DAMAGE_MULTIPLIER} */
    private double critRate;

    /** 连击率（0.0 ~ 1.0），触发后伤害 × {@code COMBO_DAMAGE_MULTIPLIER}，且本回合不切换攻击方 */
    private double comboRate;

    /** 闪避率（0.0 ~ 1.0），触发后完全免疫本次攻击 */
    private double dodgeRate;

    /** 格挡率（0.0 ~ 1.0），触发后本次伤害 × {@code BLOCK_DAMAGE_REDUCTION} */
    private double blockRate;

    /** 吸血率（0.0 ~ 1.0），触发后按造成伤害的比例回复自身生命值 */
    private double lifesteal;

    // ---- 抗性属性（削弱对方对应主动属性的实际生效概率） ----

    /** 抗暴击率，使对方有效暴击率 = max(0, 对方暴击率 - 本值) */
    private double critResistance;

    /** 抗连击率，使对方有效连击率 = max(0, 对方连击率 - 本值) */
    private double comboResistance;

    /** 抗闪避率，使对方有效闪避率 = max(0, 对方闪避率 - 本值) */
    private double dodgeResistance;

    /** 抗格挡率，使对方有效格挡率 = max(0, 对方格挡率 - 本值) */
    private double blockResistance;

    /** 抗吸血率，使对方有效吸血率 = max(0, 对方吸血率 - 本值) */
    private double lifestealResistance;

    // ---- 工厂方法 ---- 

    /**
     * 从宠物装备属性构建战斗属性
     *
     * @param level      宠物等级
     * @param stats      宠物装备属性汇总（可为 null，表示无装备）
     * @param baseAtk    攻击力基础值（对应 {@code BattleConstant.BASE_ATK}）
     * @param growthRate 攻击力指数成长率（对应 {@code BattleConstant.GROWTH_RATE}）
     * @return 构建好的战斗属性对象
     */
    public static BattleStatsVO fromPet(int level,
                                        com.cong.fishisland.model.vo.pet.PetEquipStatsVO stats,
                                        int baseAtk, double growthRate) {
        BattleStatsVO s = new BattleStatsVO();
        int equipAtk = getInt(stats != null ? stats.getTotalBaseAttack() : null);
        int equipHp  = getInt(stats != null ? stats.getTotalBaseHp()     : null);
        s.attack  = (int) (baseAtk * Math.pow(1 + growthRate, level)) + equipAtk;
        s.health  = level * 100 + equipHp;
        s.defense = getInt(stats != null ? stats.getTotalBaseDefense() : null);
        s.speed   = getInt(stats != null ? stats.getSpeed()            : null);

        s.critRate            = getDbl(stats != null ? stats.getCritRate()            : null);
        s.comboRate           = getDbl(stats != null ? stats.getComboRate()           : null);
        s.dodgeRate           = getDbl(stats != null ? stats.getDodgeRate()           : null);
        s.blockRate           = getDbl(stats != null ? stats.getBlockRate()           : null);
        s.lifesteal           = getDbl(stats != null ? stats.getLifesteal()           : null);
        s.critResistance      = getDbl(stats != null ? stats.getCritResistance()      : null);
        s.comboResistance     = getDbl(stats != null ? stats.getComboResistance()     : null);
        s.dodgeResistance     = getDbl(stats != null ? stats.getDodgeResistance()     : null);
        s.blockResistance     = getDbl(stats != null ? stats.getBlockResistance()     : null);
        s.lifestealResistance = getDbl(stats != null ? stats.getLifestealResistance() : null);
        return s;
    }

    /**
     * 从 BossVO 构建战斗属性
     * <p>Boss 没有防御力，health 取 Boss 的最大生命值。
     *
     * @param boss Boss信息VO
     * @return 构建好的战斗属性对象
     */
    public static BattleStatsVO fromBoss(com.cong.fishisland.model.vo.game.BossVO boss) {
        BattleStatsVO s = new BattleStatsVO();
        s.attack  = getInt(boss.getAttack());
        s.health  = getInt(boss.getHealth());
        s.defense = 0;

        s.critRate            = getDbl(boss.getCritRate());
        s.comboRate           = getDbl(boss.getComboRate());
        s.dodgeRate           = getDbl(boss.getDodgeRate());
        s.blockRate           = getDbl(boss.getBlockRate());
        s.lifesteal           = getDbl(boss.getLifesteal());
        s.critResistance      = getDbl(boss.getCritResistance());
        s.comboResistance     = getDbl(boss.getComboResistance());
        s.dodgeResistance     = getDbl(boss.getDodgeResistance());
        s.blockResistance     = getDbl(boss.getBlockResistance());
        s.lifestealResistance = getDbl(boss.getLifestealResistance());
        return s;
    }

    // ---- 内部工具 ----

    /**
     * 根据速度判断先手
     * <p>速度高的一方先攻击；速度相同时随机决定（50% 概率）。
     *
     * @param a   一方战斗属性
     * @param b   另一方战斗属性
     * @param rng 随机数生成器
     * @return true 表示 a 先手，false 表示 b 先手
     */
    public static boolean aGoesFirst(BattleStatsVO a, BattleStatsVO b, java.util.Random rng) {
        if (a.speed != b.speed) {
            return a.speed > b.speed;
        }
        return rng.nextBoolean();
    }

    private static int    getInt(Integer val) { return val != null ? val  : 0;   }
    private static double getDbl(Double  val) { return val != null ? val  : 0.0; }
}
