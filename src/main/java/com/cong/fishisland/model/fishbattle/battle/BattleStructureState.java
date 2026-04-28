package com.cong.fishisland.model.fishbattle.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务端权威建筑运行时状态。
 * 覆盖防御塔（tower）、水晶枢纽（nexus）和兵营水晶（inhibitor）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleStructureState {
    /** 建筑唯一标识，如 blue_outer_tower、red_nexus。 */
    private String id;

    /** 建筑类型：tower / nexus / inhibitor。 */
    private String type;

    /** 塔子类型（仅 tower 有效）：outer / inner / nexusGuard。 */
    private String subType;

    /** 所属队伍：blue / red。 */
    private String team;

    /** 建筑中心位置。 */
    private BattleVector3 position;

    /** 碰撞半径。 */
    private Double collisionRadius;

    /** 当前生命值。 */
    private Double hp;

    /** 最大生命值。 */
    private Double maxHp;

    /** 是否已被摧毁。 */
    private Boolean isDestroyed;

    /** 护甲值（用于物理减伤）。 */
    private Double armor;

    /** 攻击力（防御塔攻击小兵/英雄）。 */
    private Double attackDamage;

    /** 攻击范围。 */
    private Double attackRange;

    /** 攻击速度（每秒攻击次数）。 */
    private Double attackSpeed;

    /** 上次攻击时间戳（System.currentTimeMillis），用于攻击冷却计算。 */
    private Long lastAttackAt;

    /** 当前攻击目标的实体 ID（小兵/英雄），用于前端渲染弹道和攻击范围指示。 */
    private String targetEntityId;
}
