package com.cong.fishisland.model.entity.pay;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 虎皮椒支付订单记录
 *
 * @TableName pay_order
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@TableName(value = "pay_order")
@Data
public class PayOrder implements Serializable {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField(value = "userId")
    private Long userId;

    /**
     * 商户订单号（本系统生成，唯一）
     */
    @TableField(value = "tradeOrderId")
    private String tradeOrderId;

    /**
     * 支付平台交易号
     */
    @TableField(value = "transactionId")
    private String transactionId;

    /**
     * 虎皮椒内部订单号
     */
    @TableField(value = "openOrderId")
    private String openOrderId;

    /**
     * 订单标题
     */
    @TableField(value = "title")
    private String title;

    /**
     * 订单金额（元）
     */
    @TableField(value = "totalFee")
    private BigDecimal totalFee;

    /**
     * 订单状态：PENDING-待支付，OD-已支付，CD-已退款，RD-退款中，UD-退款失败，CLOSED-已关闭
     */
    @TableField(value = "status")
    private String status;

    /**
     * 回调通知地址
     */
    @TableField(value = "notifyUrl")
    private String notifyUrl;

    /**
     * 支付成功跳转地址
     */
    @TableField(value = "returnUrl")
    private String returnUrl;

    /**
     * 备注/附加数据，回调时原样返回
     */
    @TableField(value = "attach")
    private String attach;

    /**
     * 支付二维码地址（PC端）
     */
    @TableField(value = "urlQrcode")
    private String urlQrcode;

    /**
     * 支付跳转链接（手机端）
     */
    @TableField(value = "payUrl")
    private String payUrl;

    /**
     * 回调通知时间
     */
    @TableField(value = "notifyTime")
    private Date notifyTime;

    /**
     * 创建时间
     */
    @TableField(value = "createTime")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "updateTime")
    private Date updateTime;

    /**
     * 是否删除（0-未删除，1-已删除）
     */
    @TableLogic
    @TableField(value = "isDelete")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
