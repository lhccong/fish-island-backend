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
 * 农场 buff 道具持有表：每个用户每类道具最多 1 条（uk_user_buff 唯一键兜底）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("farm_user_buff")
@ApiModel(description = "农场 buff 道具持有实体")
public class FarmUserBuff {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "道具类型：1-生长加速 2-积分增产 3-守护 4-神奇生长")
    private Integer buffType;

    @ApiModelProperty(value = "当前等级，1 起")
    private Integer level = 1;

    @ApiModelProperty(value = "首次购买时间")
    private LocalDateTime unlockTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
