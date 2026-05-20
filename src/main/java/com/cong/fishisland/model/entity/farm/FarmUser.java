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

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "农场用户ID")
    private Long id;

    @ApiModelProperty(value = "关联的系统用户ID")
    private Long userId;

    @ApiModelProperty(value = "农场昵称")
    private String nickname;

    @ApiModelProperty(value = "农场头像")
    private String avatar;

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

    @ApiModelProperty(value = "最后签到日期")
    private LocalDateTime lastSignInDate;

    @ApiModelProperty(value = "连续签到天数")
    private Integer consecutiveDays = 0;

    @ApiModelProperty(value = "状态（0-禁用，1-正常）")
    private Integer status = 1;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;
}
