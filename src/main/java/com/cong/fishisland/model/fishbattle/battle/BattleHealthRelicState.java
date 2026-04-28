package com.cong.fishisland.model.fishbattle.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 补血道具（生命遗迹 / Health Relic）运行时状态。
 * 参考英雄联盟"生命遗迹"设计：固定点位刷新，拾取后回复生命值，一段时间后重新生成。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleHealthRelicState {

    /** 道具实例 ID，如 "relic_0"。 */
    private String id;

    /** 世界坐标。 */
    private BattleVector3 position;

    /** 当前是否可拾取。 */
    private Boolean isAvailable;

    /** 不可拾取时：重新刷新的时间戳（System.currentTimeMillis）。 */
    private Long respawnAt;

    /** 拾取后回复的生命值百分比（0~1），如 0.15 表示回复最大生命值的 15%。 */
    private Double healPercent;

    /** 拾取范围半径。 */
    private Double pickupRadius;
}
