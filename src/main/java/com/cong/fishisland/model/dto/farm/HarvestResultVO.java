package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量收获结果：地块列表 + 本次总积分 + 新解锁图鉴
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "批量收获结果")
public class HarvestResultVO {

    @ApiModelProperty(value = "收获后的地块列表")
    private List<LandDTO> lands;

    @ApiModelProperty(value = "本次收获总积分")
    private Integer totalPoints;

    @ApiModelProperty(value = "本次新解锁的图鉴（可为空）")
    private List<CollectionUnlockVO> newCollections;
}
