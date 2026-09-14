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
 * 作物图鉴表：同用户同作物同品级仅一条记录，只保留该品级最大重量
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("farm_collection_grade")
@ApiModel(description = "作物图鉴实体")
public class FarmCollectionGrade {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "作物ID")
    private Long cropId;

    @ApiModelProperty(value = "图鉴品级 1-10（与种子稀有度 rarity 无关）")
    private Integer grade;

    @ApiModelProperty(value = "该品级已记录的最大重量（克）")
    private Integer maxWeight;

    @ApiModelProperty(value = "首次解锁该品级时间")
    private LocalDateTime firstObtainedTime;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
