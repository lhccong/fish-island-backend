package com.cong.fishisland.service.impl.pay;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.config.XunhuPayConfig;
import com.cong.fishisland.mapper.pay.PayOrderMapper;
import com.cong.fishisland.model.dto.donation.DonationRecordsAddRequest;
import com.cong.fishisland.model.dto.pay.PayCreateRequest;
import com.cong.fishisland.model.dto.user.UserVipAddRequest;
import com.cong.fishisland.model.entity.donation.DonationRecords;
import com.cong.fishisland.model.dto.pay.PayOrderAttach;
import com.cong.fishisland.model.dto.pay.XunhuPayNotifyRequest;
import com.cong.fishisland.model.enums.pay.PayOrderTypeEnum;
import com.cong.fishisland.constant.VipTypeConstant;
import com.cong.fishisland.model.ws.request.Message;
import com.cong.fishisland.model.ws.request.MessageWrapper;
import com.cong.fishisland.model.ws.request.Sender;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import com.cong.fishisland.model.enums.MessageTypeEnum;
import com.cong.fishisland.service.DonationDetailRecordsService;
import com.cong.fishisland.service.DonationRecordsService;
import com.cong.fishisland.service.EventRemindService;
import com.cong.fishisland.service.RoomMessageService;
import com.cong.fishisland.service.UserTitleService;
import com.cong.fishisland.service.UserVipService;
import com.cong.fishisland.websocket.service.WebSocketService;
import com.cong.fishisland.model.entity.pay.PayOrder;
import com.cong.fishisland.model.entity.chat.RoomMessage;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.pay.PayOrderVO;
import com.cong.fishisland.service.PayOrderService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.utils.XunhuPayUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 虎皮椒支付订单 Service 实现
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Service
@Slf4j
public class PayOrderServiceImpl extends ServiceImpl<PayOrderMapper, PayOrder>
        implements PayOrderService {

    /** 订单状态：待支付 */
    private static final String STATUS_PENDING = "PENDING";
    /** 订单状态：已支付 */
    private static final String STATUS_PAID = "OD";

    @Resource
    private XunhuPayConfig xunhuPayConfig;

    @Resource
    private DonationRecordsService donationRecordsService;

    @Resource
    private DonationDetailRecordsService donationDetailRecordsService;

    @Resource
    private UserTitleService userTitleService;

    @Resource
    private UserVipService userVipService;

    @Resource
    private EventRemindService eventRemindService;

    @Resource
    private WebSocketService webSocketService;

    @Resource
    private RoomMessageService roomMessageService;

    @Resource
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayOrderVO createPayOrder(PayCreateRequest request) {
        // 参数校验
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        ThrowUtils.throwIf(request.getType() == null, ErrorCode.PARAMS_ERROR, "支付类型不能为空");
        ThrowUtils.throwIf(request.getTotalFee() == null || request.getTotalFee().compareTo(BigDecimal.ZERO) <= 0,
                ErrorCode.PARAMS_ERROR, "订单金额必须大于 0");

        // 根据 type 解析订单标题（枚举内部会校验非法值）
        PayOrderTypeEnum orderType = PayOrderTypeEnum.getEnumByValue(request.getType());
        String title = orderType.getText();

        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        String tradeOrderId = UUID.randomUUID().toString().replace("-", "");

        // 组装 attach：将用户 ID 和备注打包成 JSON，回调时原样返回，方便业务处理
        PayOrderAttach attachObj = new PayOrderAttach();
        attachObj.setUserId(userId);
        attachObj.setRemark(request.getRemark());
        String attach = JSON.toJSONString(attachObj);

        // 确定回调和跳转地址
        String notifyUrl = xunhuPayConfig.getNotifyUrl();
        String returnUrl = StringUtils.isNotBlank(request.getReturnUrl())
                ? request.getReturnUrl()
                : xunhuPayConfig.getReturnUrl();

        ThrowUtils.throwIf(StringUtils.isBlank(notifyUrl), ErrorCode.SYSTEM_ERROR, "支付回调地址未配置");

        // 构建请求参数
        Map<String, Object> params = new HashMap<>(16);
        params.put("version", "1.1");
        params.put("appid", xunhuPayConfig.getAppid());
        params.put("trade_order_id", tradeOrderId);
        params.put("total_fee", request.getTotalFee());
        params.put("title", title);
        params.put("time", XunhuPayUtils.currentSecondTimestamp());
        params.put("notify_url", notifyUrl);
        if (StringUtils.isNotBlank(returnUrl)) {
            params.put("return_url", returnUrl);
        }
        // attach 始终传入（至少包含 userId）
        params.put("attach", attach);
        params.put("nonce_str", XunhuPayUtils.randomNonceStr(9));
        params.put("plugins", "fish-island");
        // 生成签名
        params.put("hash", XunhuPayUtils.createSign(params, xunhuPayConfig.getAppsecret()));

        log.info("[XunhuPay] 发起支付请求，tradeOrderId={}, totalFee={}", tradeOrderId, request.getTotalFee());

        // 调用虎皮椒接口
        JSONObject result = XunhuPayUtils.doPayRequest(params, xunhuPayConfig.getGatewayUrl());
        ThrowUtils.throwIf(result == null, ErrorCode.SYSTEM_ERROR, "支付接口请求失败");

        Integer errcode = result.getInteger("errcode");
        String errmsg = result.getString("errmsg");
        ThrowUtils.throwIf(errcode == null || errcode != 0,
                ErrorCode.OPERATION_ERROR, "支付接口返回错误：" + errmsg);

        String urlQrcode = result.getString("url_qrcode");
        String payUrl = result.getString("url");

        // 保存订单记录
        PayOrder payOrder = new PayOrder();
        payOrder.setUserId(userId);
        payOrder.setTradeOrderId(tradeOrderId);
        payOrder.setTitle(title);
        payOrder.setTotalFee(request.getTotalFee());
        payOrder.setStatus(STATUS_PENDING);
        payOrder.setNotifyUrl(notifyUrl);
        payOrder.setReturnUrl(returnUrl);
        payOrder.setAttach(attach);
        payOrder.setUrlQrcode(urlQrcode);
        payOrder.setPayUrl(payUrl);
        payOrder.setCreateTime(new Date());
        payOrder.setUpdateTime(new Date());
        this.save(payOrder);

        // 构建返回 VO
        PayOrderVO vo = new PayOrderVO();
        BeanUtils.copyProperties(payOrder, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleNotify(XunhuPayNotifyRequest notify) {
        log.info("[XunhuPay] 收到支付回调，tradeOrderId={}, status={}", notify.getTrade_order_id(), notify.getStatus());

        // 验签
        if (!XunhuPayUtils.verifySign(notify, xunhuPayConfig.getAppsecret())) {
            log.warn("[XunhuPay] 回调签名验证失败，notify={}", notify);
            return "sign error";
        }

        String tradeOrderId = notify.getTrade_order_id();
        String status = notify.getStatus();

        if (StringUtils.isBlank(tradeOrderId) || StringUtils.isBlank(status)) {
            log.warn("[XunhuPay] 回调参数缺失，tradeOrderId={}, status={}", tradeOrderId, status);
            return "params error";
        }

        // 查询本地订单
        PayOrder payOrder = this.getOne(new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getTradeOrderId, tradeOrderId));
        if (payOrder == null) {
            log.warn("[XunhuPay] 订单不存在，tradeOrderId={}", tradeOrderId);
            return "order not found";
        }

        // 幂等：已处理过的订单直接返回 success
        if (!STATUS_PENDING.equals(payOrder.getStatus())) {
            log.info("[XunhuPay] 订单已处理，tradeOrderId={}, currentStatus={}", tradeOrderId, payOrder.getStatus());
            return "success";
        }

        // 更新订单状态
        payOrder.setStatus(status);
        payOrder.setTransactionId(notify.getTransaction_id());
        payOrder.setOpenOrderId(notify.getOpen_order_id());
        payOrder.setNotifyTime(new Date());
        payOrder.setUpdateTime(new Date());
        this.updateById(payOrder);

        // 支付成功后的业务处理（如发放积分、开通会员等）
        if (STATUS_PAID.equals(status)) {
            log.info("[XunhuPay] 订单支付成功，tradeOrderId={}, transactionId={}", tradeOrderId, notify.getTransaction_id());
            onPaySuccess(payOrder);
        }

        return "success";
    }

    @Override
    public PayOrderVO queryOrder(String tradeOrderId) {
        ThrowUtils.throwIf(StringUtils.isBlank(tradeOrderId), ErrorCode.PARAMS_ERROR, "订单号不能为空");
        PayOrder payOrder = this.getOne(new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getTradeOrderId, tradeOrderId));
        ThrowUtils.throwIf(payOrder == null, ErrorCode.NOT_FOUND_ERROR, "订单不存在");

        // 仅允许查询自己的订单
        Long loginUserId = Long.parseLong(StpUtil.getLoginId().toString());
        ThrowUtils.throwIf(!payOrder.getUserId().equals(loginUserId), ErrorCode.NO_AUTH_ERROR, "无权查询该订单");

        PayOrderVO vo = new PayOrderVO();
        BeanUtils.copyProperties(payOrder, vo);
        return vo;
    }

    /**
     * 支付成功后的业务扩展点
     * <p>
     * attach 格式为 JSON：{"userId": 123, "remark": "用户备注"}
     * 根据 title 判断业务类型，执行对应逻辑。
     *
     * @param payOrder 已支付的订单
     */
    private void onPaySuccess(PayOrder payOrder) {
        // 反序列化 attach，取出 userId 和 remark
        PayOrderAttach attach = JSON.parseObject(payOrder.getAttach(), PayOrderAttach.class);
        Long userId = attach != null ? attach.getUserId() : payOrder.getUserId();
        String remark = attach != null ? attach.getRemark() : null;

        log.info("[XunhuPay] 触发支付成功业务处理，orderId={}, userId={}, title={}, totalFee={}, remark={}",
                payOrder.getId(), userId, payOrder.getTitle(), payOrder.getTotalFee(), remark);

        if (PayOrderTypeEnum.SPONSOR.getText().equals(payOrder.getTitle())) {
            handleSponsor(userId, payOrder.getTotalFee(), remark);
        }
    }

    /**
     * 处理赞助摸鱼岛业务
     * <p>
     * 1. 更新打赏榜记录（累加金额）
     * 2. 本次金额 >= 1 元时派发赞助者称号（ID = 1）
     * 3. 查询累计打赏金额
     * 4. 累计金额 >= 29.9 元时自动派发永久 VIP，并发送系统通知
     * 5. 累计金额 >= 100 元时发送通知，提示联系岛主定制专属称号（只发一次）
     * 6. 向聊天室广播感谢消息
     *
     * @param userId   赞助用户 ID
     * @param totalFee 本次赞助金额（元）
     * @param remark   用户备注
     */
    private void handleSponsor(Long userId, BigDecimal totalFee, String remark) {
        // 1. 更新打赏榜（存在则累加，不存在则新增），并拿到累计金额
        DonationRecordsAddRequest addRequest = new DonationRecordsAddRequest();
        addRequest.setUserId(userId);
        addRequest.setAmount(totalFee);
        addRequest.setRemark(remark);
        try {
            donationRecordsService.createRecord(addRequest);
            log.info("[XunhuPay] 打赏榜更新成功，userId={}, amount={}", userId, totalFee);
        } catch (Exception e) {
            log.error("[XunhuPay] 打赏榜更新失败，userId={}, amount={}", userId, totalFee, e);
        }

        // 2. 插入打赏明细（每次独立一条，不累加，用于前端流水展示）
        try {
            donationDetailRecordsService.addDetail(userId, totalFee, remark);
            log.info("[XunhuPay] 打赏明细记录成功，userId={}, amount={}", userId, totalFee);
        } catch (Exception e) {
            log.error("[XunhuPay] 打赏明细记录失败，userId={}, amount={}", userId, totalFee, e);
        }

        // 3. 本次金额 >= 1 元时派发赞助者称号（ID = 1），已拥有则跳过
        if (totalFee != null && totalFee.compareTo(BigDecimal.ONE) >= 0) {
            try {
                boolean granted = userTitleService.addTitleToUser(userId, 1L);
                if (granted) {
                    log.info("[XunhuPay] 赞助者称号派发成功，userId={}", userId);
                } else {
                    log.info("[XunhuPay] 用户已拥有赞助者称号，跳过派发，userId={}", userId);
                }
            } catch (Exception e) {
                log.error("[XunhuPay] 赞助者称号派发失败，userId={}", userId, e);
            }
        }

        // 4. 查询累计打赏金额，用于后续多个阈值判断
        BigDecimal totalAmount = BigDecimal.ZERO;
        try {
            DonationRecords record =
                    donationRecordsService.getOne(
                            new LambdaQueryWrapper<DonationRecords>()
                                    .eq(DonationRecords::getUserId, userId));
            totalAmount = record != null ? record.getAmount() : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("[XunhuPay] 查询累计打赏金额失败，userId={}", userId, e);
        }

        // 5. 累计打赏金额 >= 29.9 元时派发永久 VIP
        try {
            BigDecimal vipThreshold = new BigDecimal("29.9");
            if (totalAmount.compareTo(vipThreshold) >= 0 && !userVipService.isPermanentVip(userId)) {
                // 派发永久 VIP（内部自动处理月卡升级）
                UserVipAddRequest vipAddRequest = new UserVipAddRequest();
                vipAddRequest.setUserId(userId);
                vipAddRequest.setType(VipTypeConstant.PERMANENT);
                userVipService.createVip(vipAddRequest);
                log.info("[XunhuPay] 永久 VIP 派发成功，userId={}, 累计打赏={}", userId, totalAmount);

                // 发送系统通知
                String vipNotify = String.format(
                        "感谢您累计赞助摸鱼岛 %.2f 元，已为您自动开通永久 VIP，感谢支持！", totalAmount);
                eventRemindService.sendSystemNotify(userId, vipNotify);
                log.info("[XunhuPay] 永久 VIP 系统通知已发送，userId={}", userId);
            }
        } catch (Exception e) {
            log.error("[XunhuPay] 永久 VIP 派发或通知失败，userId={}", userId, e);
        }

        // 6. 累计打赏金额 >= 100 元时，通知用户联系岛主定制称号（只发一次）
        try {
            BigDecimal customTitleThreshold = new BigDecimal("100");
            if (totalAmount.compareTo(customTitleThreshold) >= 0) {
                // 用专属 action 标识这条通知，避免重复发送
                boolean alreadySent = eventRemindService.existsEvent(
                        "sponsor_custom_title", -1L, 0, -1L, userId);
                if (!alreadySent) {
                    String customTitleNotify = "您的累计赞助已超过 100 元，感谢您对摸鱼岛的大力支持！" +
                            "您可以联系岛主为您定制专属称号，请私信岛主或加入官方群联系。";
                    eventRemindService.sendSystemNotify(userId, customTitleNotify, "sponsor_custom_title");
                    log.info("[XunhuPay] 定制称号通知已发送，userId={}, 累计打赏={}", userId, totalAmount);
                } else {
                    log.info("[XunhuPay] 定制称号通知已发送过，跳过，userId={}", userId);
                }
            }
        } catch (Exception e) {
            log.error("[XunhuPay] 定制称号通知发送失败，userId={}", userId, e);
        }

        // 7. 向聊天室广播感谢消息
        try {
            sendSponsorChatMessage(userId, totalFee, remark);
        } catch (Exception e) {
            log.error("[XunhuPay] 聊天室感谢消息发送失败，userId={}", userId, e);
        }
    }

    /**
     * 向聊天室广播赞助感谢消息（以摸鱼助手身份发出）
     *
     * @param userId   赞助用户 ID
     * @param totalFee 本次赞助金额（元）
     * @param remark   用户备注
     */
    private void sendSponsorChatMessage(Long userId, BigDecimal totalFee, String remark) {
        // 查询赞助用户信息，用于消息展示
        User user = userService.getById(userId);
        String userName = user != null ? user.getUserName() : "一位热心岛民";

        // 构造消息内容
        StringBuilder content = new StringBuilder();
        content.append("🎉 感谢 ").append(userName)
                .append(" 赞助摸鱼岛 ").append(totalFee).append(" 元！");
        if (StringUtils.isNotBlank(remark)) {
            content.append(" 留言：「").append(remark).append("」");
        }
        content.append(" 岛主和全体岛民感谢您的支持！❤️");

        // 构造发送者（摸鱼助手）
        Sender aiSender = Sender.builder()
                .id("-1")
                .level(1)
                .name("摸鱼助手")
                .isAdmin(false)
                .points(-999)
                .avatar("https://oss.cqbo.com/moyu/user_avatar/1/hYskW0jH-34eaba5c-3809-45ef-a3bd-dd01cf97881b_478ce06b6d869a5a11148cf3ee119bac.gif")
                .build();

        // 构造消息体
        Message message = new Message();
        message.setId(String.valueOf(System.currentTimeMillis()));
        message.setContent(content.toString());
        message.setSender(aiSender);
        message.setTimestamp(String.valueOf(System.currentTimeMillis()));

        MessageWrapper messageWrapper = new MessageWrapper();
        messageWrapper.setMessage(message);

        // 广播给所有在线用户
        webSocketService.sendToAllOnline(WSBaseResp.builder()
                .type(MessageTypeEnum.CHAT.getType())
                .data(messageWrapper).build());

        // 持久化到聊天记录
        RoomMessage roomMessage = new RoomMessage();
        roomMessage.setUserId(-1L);
        roomMessage.setRoomId(-1L);
        roomMessage.setMessageJson(JSON.toJSONString(messageWrapper));
        roomMessage.setMessageId(message.getId());
        roomMessageService.save(roomMessage);

        log.info("[XunhuPay] 聊天室感谢消息已发送，userId={}, content={}", userId, content);
    }
}
