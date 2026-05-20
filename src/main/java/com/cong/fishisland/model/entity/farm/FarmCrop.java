package  com.cong.fishisland.model.entity.farm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * @description: 作物表
 * @author: xiayuchen
 * @date: 2026/5/8 13:37
 * @param:
 * @return:
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("farm_crop")
@ApiModel(description = "农场作物实体")
public class FarmCrop {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "作物ID")
    private Long id;

    @ApiModelProperty(value = "作物名称")
    private String name;

    @ApiModelProperty(value = "作物分类（粮食/蔬菜/水果/花卉/特产）")
    private String category;

    @ApiModelProperty(value = "生长时间（分钟）")
    private Integer growthTime;

    @ApiModelProperty(value = "收获经验")
    private Integer experience;

    @ApiModelProperty(value = "收获积分")
    private Integer coin;

    @ApiModelProperty(value = "购买价格（积分）")
    private Integer price;

    @ApiModelProperty(value = "稀有度")
    private Integer rarity = 1;

    @ApiModelProperty(value = "作物图标")
    private String icon = "";

    @ApiModelProperty(value = "作物描述")
    private String description = "";

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;
}
