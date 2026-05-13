package com.cong.fishisland.model.dto.pay;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 发起支付请求
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Data
@ApiModel(value = "PayCreateRequest", description = "发起支付请求")
public class PayCreateRequest implements Serializable {

    /**
     * 支付类型：1-赞助摸鱼岛
     *
     * @see com.cong.fishisland.model.enums.pay.PayOrderTypeEnum
     */
    @ApiModelProperty(value = "支付类型：1-赞助摸鱼岛", required = true, example = "1")
    private Integer type;

    /**
     * 订单金额（元），精确到分
     */
    @ApiModelProperty(value = "订单金额（元）", required = true, example = "9.90")
    private BigDecimal totalFee;

    /**
     * 支付成功后跳转地址（可选）
     */
    @ApiModelProperty(value = "支付成功后跳转地址（可选）")
    private String returnUrl;

    /**
     * 用户备注信息（可选），会和用户 ID 一起存入 attach 字段，回调时原样返回
     */
    @ApiModelProperty(value = "用户备注信息（可选）", example = "感谢摸鱼岛，加油！")
    private String remark;

    private static final long serialVersionUID = 1L;
}
