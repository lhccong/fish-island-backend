package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "偷菜记录VO")
public class FarmStealRecordVO {

    @ApiModelProperty(value = "偷菜记录ID")
    private Long id;

    @ApiModelProperty(value = "偷菜者农场用户ID")
    private Long stealerId;

    @ApiModelProperty(value = "偷菜者昵称")
    private String stealerNickname;

    @ApiModelProperty(value = "偷菜者头像")
    private String stealerAvatar;

    @ApiModelProperty(value = "农场主人农场用户ID")
    private Long ownerId;

    @ApiModelProperty(value = "种植记录ID")
    private Long plantRecordId;

    @ApiModelProperty(value = "作物ID")
    private Long cropId;

    @ApiModelProperty(value = "偷菜时间")
    private LocalDateTime stolenTime;

    @ApiModelProperty(value = "获得的经验")
    private Integer expGained;

    @ApiModelProperty(value = "获得的积分")
    private Integer coinGained;

    @ApiModelProperty(value = "作物名称")
    private String cropName;
}
