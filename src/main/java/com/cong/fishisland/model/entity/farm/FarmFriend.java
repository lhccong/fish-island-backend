package com.cong.fishisland.model.entity.farm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName("farm_friend")
@Builder
@ApiModel(description = "农场好友关系实体")
public class FarmFriend {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "好友关系ID")
    private Long id;

    @ApiModelProperty(value = "农场用户ID（关联farm_user表的id）")
    private Long userId;

    @ApiModelProperty(value = "好友农场用户ID（关联farm_user表的id）")
    private Long friendId;

    @ApiModelProperty(value = "好友关系状态（0-拉黑，1-正常）")
    private Integer status = 1;

    @ApiModelProperty(value = "最后访问时间")
    private LocalDateTime lastVisitTime;

    @ApiModelProperty(value = "偷菜冷却时间")
    private LocalDateTime stealCooldown;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;
}
