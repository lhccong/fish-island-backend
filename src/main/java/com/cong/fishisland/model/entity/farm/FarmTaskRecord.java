package com.cong.fishisland.model.entity.farm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @description: 任务完成记录表
 * @author: xiayuchen
 * @date: 2026/5/8 13:35
 * @param:
 * @return:
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("farm_task_record")
@ApiModel(description = "农场任务完成记录实体")
public class FarmTaskRecord {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "任务记录ID")
    private Long id;

    @ApiModelProperty(value = "农场用户ID（关联farm_user表的id）")
    private Long userId;

    @ApiModelProperty(value = "任务ID")
    private Long taskId;

    @ApiModelProperty(value = "当前进度次数")
    private Integer currentCount = 0;

    @ApiModelProperty(value = "是否已完成（0-未完成，1-已完成）")
    private Integer completed = 0;

    @ApiModelProperty(value = "是否已领取奖励（0-未领取，1-已领取）")
    private Integer claimed = 0;

    @ApiModelProperty(value = "任务日期")
    private LocalDate date;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;
}
