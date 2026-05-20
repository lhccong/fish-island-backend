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

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("farm_steal_record")
@ApiModel(description = "农场偷菜记录实体")
public class FarmStealRecord {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "偷菜记录ID")
    private Long id;

    @ApiModelProperty(value = "偷菜者农场用户ID（关联farm_user表的id）")
    private Long stealerId;

    @ApiModelProperty(value = "农场主人农场用户ID（关联farm_user表的id）")
    private Long ownerId;

    @ApiModelProperty(value = "种植记录ID")
    private Long plantRecordId;

    @ApiModelProperty(value = "作物ID")
    private Long cropId;

    @ApiModelProperty(value = "偷菜时间")
    private LocalDateTime stolenTime;

    @ApiModelProperty(value = "获得的经验")
    private Integer expGained = 0;

    @ApiModelProperty(value = "获得的积分")
    private Integer coinGained = 0;
}
