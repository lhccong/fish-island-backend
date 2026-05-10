package com.cong.fishisland.controller.pay;

import com.cong.fishisland.annotation.NoRepeatSubmit;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.pay.PayCreateRequest;
import com.cong.fishisland.model.dto.pay.XunhuPayNotifyRequest;
import com.cong.fishisland.model.vo.pay.PayOrderVO;
import com.cong.fishisland.service.PayOrderService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 虎皮椒支付接口
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@RestController
@RequestMapping("/pay")
@Slf4j
public class PayOrderController {

    @Resource
    private PayOrderService payOrderService;

    /**
     * 发起支付
     * <p>
     * 创建订单并调用虎皮椒接口，返回支付二维码地址（PC端）和跳转链接（手机端）。
     */
    @PostMapping("/create")
    @ApiOperation(value = "发起支付", notes = "创建支付订单，返回二维码地址（PC端）和跳转链接（手机端）")
    @NoRepeatSubmit
    public BaseResponse<PayOrderVO> createPayOrder(@RequestBody PayCreateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        return ResultUtils.success(payOrderService.createPayOrder(request));
    }

    /**
     * 虎皮椒支付成功异步回调
     * <p>
     * 此接口由虎皮椒服务器主动调用，不需要用户登录态。
     * 虎皮椒以 form 表单（application/x-www-form-urlencoded）POST 回调，
     * 使用 @ModelAttribute 自动绑定到实体类。
     * 返回字符串 "success" 表示已处理，否则虎皮椒会重试最多 6 次。
     */
    @PostMapping("/notify")
    @ApiOperation(value = "支付回调（虎皮椒服务器调用）", notes = "虎皮椒支付成功后的异步通知接口，无需登录")
    public String payNotify(@ModelAttribute XunhuPayNotifyRequest notify) {
        return payOrderService.handleNotify(notify);
    }

    /**
     * 查询订单状态
     */
    @GetMapping("/query")
    @ApiOperation(value = "查询订单状态", notes = "根据商户订单号查询支付状态")
    public BaseResponse<PayOrderVO> queryOrder(@RequestParam String tradeOrderId) {
        if (StringUtils.isBlank(tradeOrderId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单号不能为空");
        }
        return ResultUtils.success(payOrderService.queryOrder(tradeOrderId));
    }
}
