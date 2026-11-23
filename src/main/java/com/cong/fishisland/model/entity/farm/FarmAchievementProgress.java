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
 * 农场成就任务进度表：同用户同任务仅一条（uk_user_task 兜底）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("farm_achievement_progress")
@ApiModel(description = "农场成就任务进度实体")
public class FarmAchievementProgress {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "成就任务ID")
    private Long taskId;

    @ApiModelProperty(value = "状态：0-进行中 1-已完成未领取 2-已领取")
    private Integer status = 0;

    @ApiModelProperty(value = "达成时间")
    private LocalDateTime completedTime;

    @ApiModelProperty(value = "领取时间")
    private LocalDateTime claimedTime;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
