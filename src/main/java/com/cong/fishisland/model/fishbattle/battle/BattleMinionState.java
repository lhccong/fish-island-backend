package com.cong.fishisland.model.fishbattle.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleMinionState {

    /** 小兵唯一标识，格式如 blue_minion_wave_1714100000_0。 */
    private String id;

    /** 所属队伍：blue / red。 */
    private String team;

    /** 所属兵线：mid / top / bot。 */
    private String lane;

    /** 小兵类型：melee（近战）/ caster（远程）/ siege（炮车）/ super（超级兵）。 */
    private String minionType;

    /** 当前世界坐标。 */
    private BattleVector3 position;

    /** 当前朝向角度（弧度），基于 atan2(dirX, dirZ)。 */
    private Double rotation;

    /** 移动速度，单位：游戏坐标/秒。 */
    private Double moveSpeed;

    /** 碰撞体半径，用于与建筑/其他单位的碰撞检测。 */
    private Double collisionRadius;

    /** 当前生命值。 */
    private Double hp;

    /** 最大生命值。 */
    private Double maxHp;

    /** 基础攻击力。 */
    private Double attackDamage;

    /** 攻击范围（不含目标碰撞半径）。 */
    private Double attackRange;

    /** 索敌范围，在此距离内搜索最近敌方目标。 */
    private Double acquisitionRange;

    private Double attackSpeed;

    /** 两次攻击之间的冷却时间，单位毫秒。 */
    private Long attackCooldownMs;

    /** 上一次发起攻击的时间戳（System.currentTimeMillis）。 */
    private Long lastAttackAt;

    /** 是否已死亡。 */
    private Boolean dead;

    /** 死亡时刻（System.currentTimeMillis），用于死亡动画延迟清除。 */
    private Long deadAt;

    /** 当前动画状态：idle / run / attack / death。 */
    private String animationState;

    /** 当前攻击目标的实体 ID（小兵/英雄/建筑）。 */
    private String targetEntityId;

    /** 当前攻击目标的类型：minion / champion / structure。 */
    private String targetType;

    /** 小兵 3D 模型 CDN 地址。 */
    private String modelUrl;
}
