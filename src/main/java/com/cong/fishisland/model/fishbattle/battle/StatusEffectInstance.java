package com.cong.fishisland.model.fishbattle.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 运行时状态实例。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusEffectInstance {
    /**
     * 状态实例 ID。
     */
    private String statusInstanceId;

    /**
     * 房间 ID。
     */
    private String roomId;

    /**
     * 状态定义 ID。
     */
    private String statusId;

    /**
     * 来源实体 ID。
     */
    private String sourceEntityId;

    /**
     * 目标实体 ID。
     */
    private String targetEntityId;

    /**
     * 当前层数。
     */
    private Integer stacks;

    /**
     * 创建时间戳。
     */
    private Long createdAt;

    /**
     * 过期时间戳。
     */
    private Long expiresAt;
}
