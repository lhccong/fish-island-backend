package com.cong.fishisland.model.entity.farm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 农场成就任务配置表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("farm_achievement_task")
@ApiModel(description = "农场成就任务配置实体")
public class FarmAchievementTask {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "成就名称")
    private String name;

    @ApiModelProperty(value = "成就描述")
    private String description;

    @ApiModelProperty(value = "类型：buff_upgrade/buff_purchase/collection_unlock/farm_level")
    private String type;

    @ApiModelProperty(value = "目标对象（buffType / cropId / grade 等）")
    private String targetId;

    @ApiModelProperty(value = "目标值（等级/条目数/品级）")
    private Integer targetCount;

    @ApiModelProperty(value = "奖励类型：1-积分 2-稀有种子")
    private Integer rewardType;

    @ApiModelProperty(value = "奖励数值")
    private Integer rewardValue;

    @ApiModelProperty(value = "状态：1-启用 0-禁用")
    private Integer status = 1;

    @ApiModelProperty(value = "排序")
    private Integer sortOrder = 0;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
