package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 农场 buff 道具 VO：商城/背包/农田左侧共用
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "农场 buff 道具状态")
public class FarmBuffVO {

    @ApiModelProperty(value = "道具类型：1-生长加速 2-积分增产 3-守护 4-神奇生长")
    private Integer buffType;

    @ApiModelProperty(value = "道具名称")
    private String name;

    @ApiModelProperty(value = "是否已拥有")
    private Boolean owned;

    @ApiModelProperty(value = "当前等级（未拥有为 0）")
    private Integer level;

    @ApiModelProperty(value = "等级上限")
    private Integer maxLevel;

    @ApiModelProperty(value = "当前效果描述，如 成熟时间 -18%")
    private String effectDesc;

    @ApiModelProperty(value = "下一级效果描述（已满级为空）")
    private String nextEffectDesc;

    @ApiModelProperty(value = "购买资格是否满足（田地数/等级门槛）")
    private Boolean unlockQualified;

    @ApiModelProperty(value = "解锁条件描述")
    private String unlockDesc;

    @ApiModelProperty(value = "购买消耗积分")
    private Integer purchaseCost;

    @ApiModelProperty(value = "升级消耗积分（已满级为空）")
    private Integer upgradeCost;

    @ApiModelProperty(value = "升级图鉴门槛：需拥有品级 ≥ N 的图鉴记录（已满级为空）")
    private Integer upgradeGradeThreshold;

    @ApiModelProperty(value = "图鉴门槛是否已满足")
    private Boolean upgradeThresholdSatisfied;
}
