package com.cong.fishisland.service.impl.chat;

import com.alibaba.fastjson.JSON;
import com.cong.fishisland.model.entity.chat.RoomMessage;
import com.cong.fishisland.model.ws.request.Message;
import com.cong.fishisland.model.ws.request.MessageWrapper;
import com.cong.fishisland.service.RoomMessageService;
import com.cong.fishisland.websocket.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RobotChatMessageServiceTest {
    private final RoomMessageService history = mock(RoomMessageService.class);
    private final RobotChatMessageService service = new RobotChatMessageService(
            mock(WebSocketService.class), history);

    @BeforeEach
    void setUp() {
        when(history.save(any(RoomMessage.class))).thenReturn(true);
    }

    @Test
    void savesSearchKeywordWithTheDialogue() {
        Message reply = service.saveBigFishReply("来啦。", "开心", true);

        ArgumentCaptor<RoomMessage> saved = ArgumentCaptor.forClass(RoomMessage.class);
        verify(history).save(saved.capture());
        Message restored = JSON.parseObject(saved.getValue().getMessageJson(), MessageWrapper.class).getMessage();
        assertEquals("开心", reply.getBigFishEmoticonKeyword());
        assertNull(reply.getBigFishEmoticonUrl());
        assertEquals(reply, restored);
    }

    @Test
    void unsampledReplyStillSavesTheDialogue() {
        Message reply = service.saveBigFishReply("来啦。", "开心", false);
        assertEquals("来啦。", reply.getContent());
        assertNull(reply.getBigFishEmoticonKeyword());
        verify(history).save(any(RoomMessage.class));
    }
}
