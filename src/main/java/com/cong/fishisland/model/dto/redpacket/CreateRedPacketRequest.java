package com.cong.fishisland.model.dto.redpacket;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


/**
 * 创建红包请求
 * @author cong
 */
@Data
//@ApiModel(value = "创建红包请求")
public class CreateRedPacketRequest {
    
    /**
     * 红包总金额（积分）
     */
    @ApiModelProperty(value = "红包总金额（积分）", required = true, example = "50")
    private Integer totalAmount;
    
    /**
     * 红包个数
     */
    @ApiModelProperty(value = "红包个数", required = true, example = "10")
    private Integer count;
    
    /**
     * 红包类型：1-随机红包，2-平均红包，3-答题红包
     */
    @ApiModelProperty(value = "红包类型：1-随机红包，2-平均红包，3-答题红包", required = true, example = "1")
    private Integer type;

    /**
     * 红包名称（答题红包时作为题目）
     */
    @ApiModelProperty(value = "红包名称（答题红包时作为题目）", example = "摸鱼岛成立于哪一年？")
    private String name;

    /**
     * 正确答案（type=3 答题红包时必填，抢红包时需提交一致答案）
     */
    @ApiModelProperty(value = "正确答案（答题红包必填）", example = "2024")
    private String answer;
} 