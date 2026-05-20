package com.cong.fishisland.model.entity.farm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("farm_user")
@Builder
@ApiModel(description = "农场用户实体")
public class FarmUser {

    @TableId(value = "userId", type = IdType.INPUT)
    @ApiModelProperty(value = "系统用户ID（主键，关联 user 表）")
    private Long userId;

    @ApiModelProperty(value = "农场等级")
    private Integer level = 1;

    @ApiModelProperty(value = "经验值")
    private Integer experience = 0;

    @ApiModelProperty(value = "总收获次数")
    private Integer totalHarvest = 0;

    @ApiModelProperty(value = "总偷菜次数")
    private Integer totalSteal = 0;

    @ApiModelProperty(value = "总防御次数")
    private Integer totalDefense = 0;

    @ApiModelProperty(value = "好友数量")
    private Integer friendCount = 0;

    @ApiModelProperty(value = "被访问次数")
    private Integer visitedCount = 0;

    @ApiModelProperty(value = "状态（0-禁用，1-正常）")
    private Integer status = 1;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
