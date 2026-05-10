package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.pay.PayCreateRequest;
import com.cong.fishisland.model.dto.pay.XunhuPayNotifyRequest;
import com.cong.fishisland.model.entity.pay.PayOrder;
import com.cong.fishisland.model.vo.pay.PayOrderVO;

/**
 * 虎皮椒支付订单 Service
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
public interface PayOrderService extends IService<PayOrder> {

    /**
     * 发起支付，创建订单并调用虎皮椒接口获取支付链接
     *
     * @param request 支付请求参数
     * @return 支付订单 VO（含二维码地址和跳转链接）
     */
    PayOrderVO createPayOrder(PayCreateRequest request);

    /**
     * 虎皮椒支付成功异步回调处理
     * <p>
     * 验签通过后更新订单状态，并触发业务逻辑（如发放积分）。
     * 返回 "success" 表示已处理，否则虎皮椒会重试最多 6 次。
     *
     * @param notify 回调参数实体（form 表单，@ModelAttribute 接收）
     * @return "success" 或错误描述
     */
    String handleNotify(XunhuPayNotifyRequest notify);

    /**
     * 查询订单状态
     *
     * @param tradeOrderId 商户订单号
     * @return 支付订单 VO
     */
    PayOrderVO queryOrder(String tradeOrderId);
}
