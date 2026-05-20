package com.cong.fishisland.model.entity.farm;

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
 * @description: 图鉴收集表
 * @author: xiayuchen
 * @date: 2026/5/8 13:36
 * @param:
 * @return:
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("farm_collection")
@ApiModel(description = "农场收集册实体")
public class FarmCollection {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "收集记录ID")
    private Long id;

    @ApiModelProperty(value = "农场用户ID（关联farm_user表的id）")
    private Long userId;

    @ApiModelProperty(value = "作物ID")
    private Long cropId;

    @ApiModelProperty(value = "是否已获得（0-未获得，1-已获得）")
    private Integer obtained = 0;

    @ApiModelProperty(value = "首次获得时间")
    private LocalDateTime obtainedTime;

    @ApiModelProperty(value = "收集次数")
    private Integer count = 0;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;
}
