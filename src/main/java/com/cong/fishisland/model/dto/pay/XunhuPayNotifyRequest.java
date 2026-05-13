package com.cong.fishisland.model.dto.pay;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 虎皮椒支付成功异步回调参数
 * <p>
 * 虎皮椒以 form 表单（application/x-www-form-urlencoded）POST 方式回调，
 * 使用 @ModelAttribute 接收，不能用 @RequestBody。
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Data
@ApiModel(value = "XunhuPayNotifyRequest", description = "虎皮椒支付回调参数")
public class XunhuPayNotifyRequest implements Serializable {

    /**
     * 商户订单号（支付时传入的 trade_order_id）
     */
    @ApiModelProperty(value = "商户订单号")
    private String trade_order_id;

    /**
     * 订单支付金额（元）
     */
    @ApiModelProperty(value = "订单支付金额（元）")
    private BigDecimal total_fee;

    /**
     * 支付平台交易号
     */
    @ApiModelProperty(value = "支付平台交易号")
    private String transaction_id;

    /**
     * 虎皮椒内部订单号
     */
    @ApiModelProperty(value = "虎皮椒内部订单号")
    private String open_order_id;

    /**
     * 订单标题
     */
    @ApiModelProperty(value = "订单标题")
    private String order_title;

    /**
     * 订单状态：OD-已支付，CD-已退款，RD-退款中，UD-退款失败
     */
    @ApiModelProperty(value = "订单状态：OD-已支付，CD-已退款，RD-退款中，UD-退款失败")
    private String status;

    /**
     * 支付渠道 APPID
     */
    @ApiModelProperty(value = "支付渠道 APPID")
    private String appid;

    /**
     * 时间戳
     */
    @ApiModelProperty(value = "时间戳")
    private String time;

    /**
     * 随机字符串
     */
    @ApiModelProperty(value = "随机字符串")
    private String nonce_str;

    /**
     * 签名（验签时不参与计算）
     */
    @ApiModelProperty(value = "签名")
    private String hash;

    /**
     * 插件 ID（传入 plugins 时才有）
     */
    @ApiModelProperty(value = "插件 ID")
    private String plugins;

    /**
     * 备注（传入 attach 时原样返回）
     */
    @ApiModelProperty(value = "备注/附加数据")
    private String attach;

    private static final long serialVersionUID = 1L;
}
