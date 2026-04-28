package com.cong.fishisland.model.fishbattle.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务端技能实例骨架。
 * 当前阶段先记录阶段、时间戳与基础目标信息，后续再扩展命中记录、投射物引用与 Buff 链接。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveSpellInstance {
    /**
     * 技能实例 ID。
     */
    private String castInstanceId;

    /**
     * 施法请求 ID。
     */
    private String requestId;

    /**
     * 房间 ID。
     */
    private String roomId;

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
     * 当前阶段。
     */
    private String stage;

    /**
     * 目标实体 ID。
     */
    private String targetEntityId;

    /**
     * 目标点。
     */
    private BattleVector3 targetPoint;

    /**
     * 目标方向。
     */
    private BattleVector3 targetDirection;

    /**
     * 创建时间戳。
     */
    private Long createdAt;

    /**
     * 当前阶段开始时间戳。
     */
    private Long stageStartedAt;

    /**
     * 前摇结束时间戳。
     */
    private Long expectedResolveAt;
}
