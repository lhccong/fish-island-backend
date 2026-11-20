package com.cong.fishisland.service.impl.chat;

import com.alibaba.fastjson.JSON;
import com.cong.fishisland.model.entity.chat.RoomMessage;
import com.cong.fishisland.model.ws.request.Message;
import com.cong.fishisland.model.ws.request.MessageWrapper;
import com.cong.fishisland.service.EmoticonFavourService;
import com.cong.fishisland.service.RoomMessageService;
import com.cong.fishisland.websocket.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RobotChatMessageServiceTest {
    private final EmoticonFavourService emoticons = mock(EmoticonFavourService.class);
    private final RoomMessageService history = mock(RoomMessageService.class);
    private final RobotChatMessageService service = new RobotChatMessageService(
            mock(WebSocketService.class), history, emoticons);

    @BeforeEach
    void setUp() {
        when(history.save(any(RoomMessage.class))).thenReturn(true);
    }

    @Test
    void samplesOnlyUserOneAndSavesTheImageWithTheDialogue() {
        String url = "https://example.test/sample.gif";
        when(emoticons.pickRandomSourceForUser(1L)).thenReturn(url);

        Message reply = service.saveBigFishReply("来啦。", true);

        verify(emoticons).pickRandomSourceForUser(1L);
        ArgumentCaptor<RoomMessage> saved = ArgumentCaptor.forClass(RoomMessage.class);
        verify(history).save(saved.capture());
        Message restored = JSON.parseObject(saved.getValue().getMessageJson(), MessageWrapper.class).getMessage();
        assertEquals(url, reply.getBigFishEmoticonUrl());
        assertEquals(reply, restored);
    }

    @Test
    void unsampledReplySkipsTheFavoritesQuery() {
        assertNull(service.saveBigFishReply("来啦。", false).getBigFishEmoticonUrl());
        verifyNoInteractions(emoticons);
    }

    @Test
    void missingInvalidOrUnavailableImageDoesNotPreventSavingText() {
        when(emoticons.pickRandomSourceForUser(1L)).thenReturn(null, "https://user:pass@example.test/image.gif")
                .thenThrow(new IllegalStateException());

        for (int count = 0; count < 3; count++) {
            Message reply = service.saveBigFishReply("来啦。", true);
            assertEquals("来啦。", reply.getContent());
            assertNull(reply.getBigFishEmoticonUrl());
        }
        verify(history, times(3)).save(any(RoomMessage.class));
    }
}
