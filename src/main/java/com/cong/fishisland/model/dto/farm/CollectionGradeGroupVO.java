package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 作物图鉴分组 VO：按作物聚合的图鉴展示（背包图鉴 tab）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "作物图鉴分组")
public class CollectionGradeGroupVO {

    @ApiModelProperty(value = "作物ID")
    private Long cropId;

    @ApiModelProperty(value = "作物名称")
    private String cropName;

    @ApiModelProperty(value = "作物图标")
    private String cropIcon;

    @ApiModelProperty(value = "已解锁品级条目")
    private List<CollectionGradeEntryVO> entries;

    @ApiModelProperty(value = "已解锁品级数")
    private Integer unlockedCount;

    @ApiModelProperty(value = "图鉴完成度（已解锁/10）")
    private Double completePercent;
}
