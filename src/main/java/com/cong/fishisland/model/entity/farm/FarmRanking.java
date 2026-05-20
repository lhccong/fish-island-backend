package  com.cong.fishisland.model.entity.farm;

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

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("farm_ranking")
@ApiModel(description = "农场排行榜实体")
public class FarmRanking {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "排行榜记录ID")
    private Long id;

    @ApiModelProperty(value = "农场用户ID（关联farm_user表的id）")
    private Long userId;

    @ApiModelProperty(value = "排行类型（steal_exp/steal_count/defense）")
    private String type;

    @ApiModelProperty(value = "今日数值")
    private Integer todayValue = 0;

    @ApiModelProperty(value = "总数值")
    private Integer totalValue = 0;

    @ApiModelProperty(value = "日期")
    private LocalDate date;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;
}
