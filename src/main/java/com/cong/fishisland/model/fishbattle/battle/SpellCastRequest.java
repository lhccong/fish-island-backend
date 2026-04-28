package com.cong.fishisland.model.fishbattle.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 服务端统一施法请求模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpellCastRequest {
    /**
     * 客户端请求 ID。
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
     * 技能槽位。
     */
    private String slot;

    /**
     * 技能定义 ID。
     */
    private String skillId;

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
     * 客户端时间戳。
     */
    private Long clientTimestamp;

    /**
     * 额外上下文。
     */
    private Map<String, Object> extraContext;
}
