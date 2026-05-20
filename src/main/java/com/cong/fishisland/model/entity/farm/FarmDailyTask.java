package  com.cong.fishisland.model.entity.farm;

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
 * @description: 每日任务表
 * @author: xiayuchen
 * @date: 2026/5/8 13:39
 * @param:
 * @return:
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("farm_daily_task")
@ApiModel(description = "农场每日任务实体")
public class FarmDailyTask {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "任务ID")
    private Long id;

    @ApiModelProperty(value = "任务名称")
    private String name;

    @ApiModelProperty(value = "任务描述")
    private String description = "";

    @ApiModelProperty(value = "目标次数")
    private Integer targetCount;

    @ApiModelProperty(value = "奖励经验")
    private Integer rewardExp;

    @ApiModelProperty(value = "任务类型")
    private String type;

    @ApiModelProperty(value = "排序顺序")
    private Integer sortOrder = 0;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;
}
