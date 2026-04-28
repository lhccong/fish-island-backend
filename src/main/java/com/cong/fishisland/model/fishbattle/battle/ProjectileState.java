package com.cong.fishisland.model.fishbattle.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 运行时投射物状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectileState {
    /**
     * 投射物 ID。
     */
    private String projectileId;

    /**
     * 房间 ID。
     */
    private String roomId;

    /**
     * 来源技能实例 ID。
     */
    private String castInstanceId;

    /**
     * 所有者实体 ID。
     */
    private String ownerId;

    /**
     * 技能定义 ID。
     */
    private String skillId;

    /**
     * 当前坐标。
     */
    private BattleVector3 position;

    /**
     * 当前方向。
     */
    private BattleVector3 direction;

    /**
     * 飞行速度。
     */
    private Double speed;

    /**
     * 碰撞半径。
     */
    private Double radius;

    /**
     * 是否可被拦截。
     */
    private Boolean blockable;

    /**
     * 是否已命中目标（命中后标记为 true，下一次清理时移除）。
     */
    private Boolean hit;

    /**
     * 投射物所有者所属队伍（用于判定敌我）。
     */
    private String ownerTeam;

    /**
     * 投射物命中后应执行的效果定义路径（如 onImpact 效果链）。
     * 当前阶段用技能 ID + 变体 ID 索引回技能定义进行效果执行。
     */
    private String variantId;

    /**
     * 创建时间戳。
     */
    private Long createdAt;

    /**
     * 过期时间戳。
     */
    private Long expiresAt;
}
