package com.cong.fishisland.model.fishbattle.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务端战斗坐标结构。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleVector3 {
    /**
     * X 坐标。
     */
    private double x;

    /**
     * Y 坐标。
     */
    private double y;

    /**
     * Z 坐标。
     */
    private double z;
}
