package  com.cong.fishisland.model.entity.farm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * @description: 地块表
 * @author: xiayuchen
 * @date: 2026/5/8 13:40
 * @param:
 * @return:
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("farm_land")
@Builder
@ApiModel(description = "农场地块实体")
public class FarmLand {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "地块ID")
    private Long id;

    @ApiModelProperty(value = "农场用户ID（关联farm_user表的id）")
    private Long userId;

    @ApiModelProperty(value = "地块索引")
    private Integer landIndex;

    @ApiModelProperty(value = "地块状态（0-空闲，1-种植中，2-已成熟）")
    private Integer status = 0;

    @ApiModelProperty(value = "种植的作物ID")
    private Long plantedCropId;

    @ApiModelProperty(value = "种植时间")
    private LocalDateTime plantedTime;

    @ApiModelProperty(value = "收获时间")
    private LocalDateTime harvestTime;

    @ApiModelProperty(value = "是否锁定（0-未锁定，1-已锁定）")
    private Integer locked = 0;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;
}
