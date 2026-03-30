package com.cong.fishisland.model.vo.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 用户积分记录VO
 *
 * @author cong
 */
@Data
@ApiModel(value = "用户积分记录VO")
public class UserPointsRecordVO {

    /**
     * 记录ID
     */
    @ApiModelProperty(value = "记录ID")
    private Long id;

    /**
     * 用户ID
     */
    @ApiModelProperty(value = "用户ID")
    private Long userId;

    /**
     * 变动类型：1-增加，2-扣除
     */
    @ApiModelProperty(value = "变动类型：1-增加，2-扣除")
    private Integer changeType;

    /**
     * 变动类型文本
     */
    @ApiModelProperty(value = "变动类型文本")
    private String changeTypeText;

    /**
     * 变动积分数量
     */
    @ApiModelProperty(value = "变动积分数量")
    private Integer changePoints;

    /**
     * 变动前总积分
     */
    @ApiModelProperty(value = "变动前总积分")
    private Integer beforePoints;

    /**
     * 变动后总积分
     */
    @ApiModelProperty(value = "变动后总积分")
    private Integer afterPoints;

    /**
     * 变动前已用积分
     */
    @ApiModelProperty(value = "变动前已用积分")
    private Integer beforeUsedPoints;

    /**
     * 变动后已用积分
     */
    @ApiModelProperty(value = "变动后已用积分")
    private Integer afterUsedPoints;

    /**
     * 来源类型
     */
    @ApiModelProperty(value = "来源类型")
    private String sourceType;

    /**
     * 来源类型文本
     */
    @ApiModelProperty(value = "来源类型文本")
    private String sourceTypeText;

    /**
     * 来源ID
     */
    @ApiModelProperty(value = "来源ID")
    private String sourceId;

    /**
     * 描述/备注
     */
    @ApiModelProperty(value = "描述/备注")
    private String description;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    private Date createTime;
}
