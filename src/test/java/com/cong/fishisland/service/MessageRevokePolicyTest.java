package com.cong.fishisland.service;

import com.alibaba.fastjson.JSON;
import com.cong.fishisland.model.entity.chat.RoomMessage;
import com.cong.fishisland.model.ws.request.Message;
import com.cong.fishisland.model.ws.request.MessageWrapper;
import com.cong.fishisland.service.impl.chat.MessageRevokePolicy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageRevokePolicyTest {
    private RoomMessage stored(String content) {
        Message body = new Message();
        body.setContent(content);
        MessageWrapper wrapper = new MessageWrapper();
        wrapper.setMessage(body);
        RoomMessage row = new RoomMessage();
        row.setUserId(1L);
        row.setMessageJson(JSON.toJSONString(wrapper));
        return row;
    }

    @Test void rewardsRequireAdminEvenForSender() {
        for (String tag : new String[]{"redpacket", "luckybag", "REDPACKET", "LuckyBag"}) {
            RoomMessage row = stored("前文[" + tag + "]synthetic[/" + tag + "]后文");
            assertFalse(MessageRevokePolicy.canRevoke(row, 1L, "user"));
            assertFalse(MessageRevokePolicy.canRevoke(row, 2L, "user"));
            assertTrue(MessageRevokePolicy.canRevoke(row, 1L, "admin"));
            assertTrue(MessageRevokePolicy.canRevoke(row, 2L, "admin"));
        }
    }

    @Test void normalMessagesKeepExistingPermissionsAndQuotesDoNotChangeMessageType() {
        RoomMessage row = stored("合成普通正文");
        assertTrue(MessageRevokePolicy.canRevoke(row, 1L, "user"));
        assertFalse(MessageRevokePolicy.canRevoke(row, 2L, "user"));
        assertTrue(MessageRevokePolicy.canRevoke(row, 2L, "admin"));
        assertFalse(MessageRevokePolicy.canRevoke(null, 1L, "admin"));
        assertTrue(MessageRevokePolicy.canRevoke(stored("[redpacket]not/a/card[/redpacket]"), 1L, "user"));
        row.setMessageJson("{\"message\":{\"content\":\"普通正文\",\"quotedMessage\":{\"content\":\"[redpacket]synthetic[/redpacket]\"}}}");
        assertTrue(MessageRevokePolicy.canRevoke(row, 1L, "user"));
    }

    @Test void malformedStoredContentCannotBypassRewardRestriction() {
        RoomMessage row = stored("合成");
        for (String invalid : new String[]{"invalid-json", "null", "{}", "{\"message\":{}}"}) {
            row.setMessageJson(invalid);
            assertFalse(MessageRevokePolicy.canRevoke(row, 1L, "user"));
            assertTrue(MessageRevokePolicy.canRevoke(row, 2L, "admin"));
        }
    }
}
