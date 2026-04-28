package com.cong.fishisland.model.fishbattle.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 运行时区域体状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaEffectState {
    /**
     * 区域体 ID。
     */
    private String areaId;

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
     * 区域体类型。
     */
    private String areaType;

    /**
     * 区域中心位置。
     */
    private BattleVector3 position;

    /**
     * 半径。
     */
    private Double radius;

    /**
     * 区域体绕 Y 轴旋转角度，单位弧度（用于矩形/扇形区域朝向）。
     */
    private Double rotationY;

    /**
     * 区域体长度（矩形区域或风墙等线性区域使用）。
     */
    private Double length;

    /**
     * 区域体宽度或厚度。
     */
    private Double width;

    /**
     * 区域体高度。
     */
    private Double height;

    /**
     * 创建时间戳。
     */
    private Long createdAt;

    /**
     * 过期时间戳。
     */
    private Long expiresAt;
}
