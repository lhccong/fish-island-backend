package com.cong.fishisland.service;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.controller.admin.BroadcastController;
import com.cong.fishisland.model.dto.admin.BroadcastPublishRequest;
import com.cong.fishisland.model.ws.response.BroadcastResponse;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import com.cong.fishisland.service.admin.BroadcastService;
import com.cong.fishisland.websocket.service.WebSocketService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BroadcastServiceTest {
    @Test void publishesPlainTextWithServerIdentityAndUniqueId() {
        WebSocketService socket = mock(WebSocketService.class);
        BroadcastService service = new BroadcastService(socket);
        service.publish("  <b>合成播报</b>\n  ");
        service.publish("第二条合成播报");
        ArgumentCaptor<WSBaseResp> sent = ArgumentCaptor.forClass(WSBaseResp.class);
        verify(socket, times(2)).sendToAllOnline(sent.capture());
        WSBaseResp<?> frame = sent.getAllValues().get(0);
        assertEquals("broadcast", frame.getType());
        BroadcastResponse event = (BroadcastResponse) frame.getData();
        assertEquals("<b>合成播报</b>", event.getContent());
        assertTrue(event.getPublishedAt() > 0);
        assertNotEquals(event.getId(), ((BroadcastResponse) sent.getAllValues().get(1).getData()).getId());
    }

    @Test void rejectsBlankOversizedAndControlTextBeforeSending() {
        WebSocketService socket = mock(WebSocketService.class);
        BroadcastService service = new BroadcastService(socket);
        char[] large = new char[201];
        Arrays.fill(large, 'a');
        for (String value : new String[]{null, "", " \n\t", "\u3000\u00a0", new String(large), "\u0000bad"}) {
            assertThrows(BusinessException.class, () -> service.publish(value));
        }
        verifyNoInteractions(socket);
        large[200] = ' ';
        assertDoesNotThrow(() -> service.publish(new String(large)));
    }

    @Test void protectsPublishWithAdminRoleAndDoesNotReportFailedDispatchAsSuccess() throws Exception {
        SaCheckRole guard = BroadcastController.class.getMethod("publish", BroadcastPublishRequest.class)
                .getAnnotation(SaCheckRole.class);
        assertArrayEquals(new String[]{"admin"}, guard.value());
        WebSocketService socket = mock(WebSocketService.class);
        doThrow(new IllegalStateException("synthetic dispatch failure")).when(socket).sendToAllOnline(any());
        BroadcastController controller = new BroadcastController(new BroadcastService(socket));
        BroadcastPublishRequest request = new BroadcastPublishRequest();
        request.setContent("合成播报");
        assertThrows(IllegalStateException.class, () -> controller.publish(request));
    }
}
