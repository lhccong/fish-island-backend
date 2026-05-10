package com.cong.fishisland.model.vo.pay;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付订单 VO
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Data
@ApiModel(value = "PayOrderVO", description = "支付订单信息")
public class PayOrderVO {

    /**
     * 订单ID
     */
    @ApiModelProperty(value = "订单ID")
    private Long id;

    /**
     * 商户订单号
     */
    @ApiModelProperty(value = "商户订单号")
    private String tradeOrderId;

    /**
     * 订单标题
     */
    @ApiModelProperty(value = "订单标题")
    private String title;

    /**
     * 订单金额（元）
     */
    @ApiModelProperty(value = "订单金额（元）")
    private BigDecimal totalFee;

    /**
     * 订单状态：PENDING-待支付，OD-已支付，CD-已退款，RD-退款中，UD-退款失败，CLOSED-已关闭
     */
    @ApiModelProperty(value = "订单状态")
    private String status;

    /**
     * 支付二维码地址（PC端扫码支付）
     */
    @ApiModelProperty(value = "支付二维码地址（PC端）")
    private String urlQrcode;

    /**
     * 支付跳转链接（手机端）
     */
    @ApiModelProperty(value = "支付跳转链接（手机端）")
    private String payUrl;
}
