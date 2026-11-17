package com.cong.fishisland.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cong.fishisland.config.XunhuPayConfig;
import com.cong.fishisland.model.dto.pay.XunhuPayNotifyRequest;
import com.cong.fishisland.model.entity.pay.PayOrder;
import com.cong.fishisland.model.enums.pay.PayOrderTypeEnum;
import com.cong.fishisland.service.broadcast.AchievementBroadcastService;
import com.cong.fishisland.service.impl.pay.PayOrderServiceImpl;
import com.cong.fishisland.utils.XunhuPayUtils;
import com.cong.fishisland.websocket.service.WebSocketService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SponsorBroadcastTest {
    private PayOrderServiceImpl service;
    private final AchievementBroadcastService broadcasts = mock(AchievementBroadcastService.class);
    private final DonationRecordsService donations = mock(DonationRecordsService.class);
    private final PayOrder order = new PayOrder();
    private final XunhuPayConfig config = new XunhuPayConfig();

    @BeforeEach void setup() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "sponsor-broadcast"), PayOrder.class);
        config.setAppsecret(UUID.randomUUID().toString());
        service = spy(new PayOrderServiceImpl());
        ReflectionTestUtils.setField(service, "xunhuPayConfig", config);
        ReflectionTestUtils.setField(service, "broadcasts", broadcasts);
        ReflectionTestUtils.setField(service, "donationRecordsService", donations);
        ReflectionTestUtils.setField(service, "donationDetailRecordsService", mock(DonationDetailRecordsService.class));
        ReflectionTestUtils.setField(service, "userTitleService", mock(UserTitleService.class));
        ReflectionTestUtils.setField(service, "userVipService", mock(UserVipService.class));
        ReflectionTestUtils.setField(service, "eventRemindService", mock(EventRemindService.class));
        ReflectionTestUtils.setField(service, "userService", mock(UserService.class));
        ReflectionTestUtils.setField(service, "userPointsService", mock(UserPointsService.class));
        ReflectionTestUtils.setField(service, "webSocketService", mock(WebSocketService.class));
        ReflectionTestUtils.setField(service, "roomMessageService", mock(RoomMessageService.class));
        order.setId(1L); order.setUserId(2L); order.setTradeOrderId("synthetic-order");
        order.setTitle(PayOrderTypeEnum.SPONSOR.getText());
        order.setStatus("PENDING"); order.setTotalFee(new BigDecimal("6.60"));
        doReturn(order).when(service).getOne(any());
        doReturn(true).when(service).update(any(PayOrder.class), any(Wrapper.class));
    }

    @Test void paidSponsorNotifiesOnceAndUpdateClaimsPendingStatus() {
        assertEquals("success", service.handleNotify(notify("OD")));
        assertEquals("success", service.handleNotify(notify("OD")));
        verify(broadcasts, times(1)).sponsorReceived(2L, new BigDecimal("6.60"));
        verify(donations, times(1)).createRecord(any());
        ArgumentCaptor<Wrapper> condition = ArgumentCaptor.forClass(Wrapper.class);
        verify(service).update(eq(order), condition.capture());
        assertTrue(condition.getValue().getSqlSegment().contains("status"));
        assertTrue(condition.getValue().getSqlSegment().contains("id"));
    }

    @Test void concurrentCallbackThatLosesClaimDoesNotSettleOrBroadcast() {
        doReturn(false).when(service).update(any(PayOrder.class), any(Wrapper.class));
        assertEquals("success", service.handleNotify(notify("OD")));
        verifyNoInteractions(broadcasts, donations);
    }

    @Test void invalidSignatureUnpaidAndOtherBusinessDoNotBroadcast() {
        XunhuPayNotifyRequest invalid = notify("OD");
        invalid.setHash("invalid");
        assertEquals("sign error", service.handleNotify(invalid));
        verify(service, never()).update(any(PayOrder.class), any(Wrapper.class));
        assertEquals("success", service.handleNotify(notify("RD")));
        order.setStatus("PENDING"); order.setTitle("other");
        assertEquals("success", service.handleNotify(notify("OD")));
        verifyNoInteractions(broadcasts, donations);
    }

    @Test void failedOrderWriteCannotBroadcast() {
        doThrow(new IllegalStateException()).when(service).update(any(PayOrder.class), any(Wrapper.class));
        assertThrows(IllegalStateException.class, () -> service.handleNotify(notify("OD")));
        verifyNoInteractions(broadcasts, donations);
    }

    private XunhuPayNotifyRequest notify(String status) {
        XunhuPayNotifyRequest notify = new XunhuPayNotifyRequest();
        notify.setTrade_order_id(order.getTradeOrderId());
        notify.setStatus(status);
        Map<String, Object> params = new HashMap<>();
        params.put("trade_order_id", notify.getTrade_order_id());
        params.put("status", status);
        notify.setHash(XunhuPayUtils.createSign(params, config.getAppsecret()));
        return notify;
    }
}
