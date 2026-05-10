package com.cong.fishisland.model.dto.pay;

import lombok.Data;

import java.io.Serializable;

/**
 * 支付订单 attach 附加数据
 * <p>
 * 发起支付时序列化为 JSON 写入 attach 字段，回调时反序列化使用。
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Data
public class PayOrderAttach implements Serializable {

    /**
     * 用户 ID（从登录态自动写入，不依赖前端传参）
     */
    private Long userId;

    /**
     * 用户备注信息（可选）
     */
    private String remark;

    private static final long serialVersionUID = 1L;
}
