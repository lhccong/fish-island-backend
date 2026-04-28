package com.cong.fishisland.model.fishbattle.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 技能实例阶段切换记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpellStageTransition {
    /**
     * 技能实例 ID。
     */
    private String castInstanceId;

    /**
     * 施法者实体 ID。
     */
    private String casterId;

    /**
     * 技能定义 ID。
     */
    private String skillId;

    /**
     * 技能槽位。
     */
    private String slot;

    /**
     * 目标实体 ID。
     */
    private String targetEntityId;

    /**
     * 目标点。
     */
    private BattleVector3 targetPoint;

    /**
     * 切换前阶段。
     */
    private String previousStage;

    /**
     * 切换后阶段。
     */
    private String nextStage;
}
