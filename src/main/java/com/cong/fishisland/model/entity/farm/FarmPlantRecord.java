package com.cong.fishisland.model.entity.farm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * @description: 种植记录表
 * @author: xiayuchen
 * @date: 2026/5/8 13:40
 * @param:
 * @return:
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("farm_plant_record")
@ApiModel(description = "农场种植记录实体")
public class FarmPlantRecord {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "种植记录ID")
    private Long id;

    @ApiModelProperty(value = "农场用户ID（关联farm_user表的id）")
    private Long userId;

    @ApiModelProperty(value = "地块ID")
    private Long landId;

    @ApiModelProperty(value = "作物ID")
    private Long cropId;

    @ApiModelProperty(value = "种植时间")
    private LocalDateTime plantedTime;

    @ApiModelProperty(value = "预计收获时间")
    private LocalDateTime harvestTime;

    @ApiModelProperty(value = "是否已收获（0-未收获，1-已收获）")
    private Integer harvested = 0;

    @ApiModelProperty(value = "实际收获时间")
    private LocalDateTime harvestedTime;

    @ApiModelProperty(value = "种植时预期积分奖励")
    private Integer plantedPointsReward;

    @ApiModelProperty(value = "被偷积分总数")
    private Integer stolenPoints = 0;

    @ApiModelProperty(value = "被偷次数")
    private Integer stolenCount = 0;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;
}
