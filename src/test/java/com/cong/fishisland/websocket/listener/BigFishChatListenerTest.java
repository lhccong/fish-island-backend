package com.cong.fishisland.websocket.listener;

import com.alibaba.fastjson.JSON;
import com.cong.fishisland.datasource.ai.BigFishDataSource;
import com.cong.fishisland.datasource.ai.BigFishMemoryClassifier;
import com.cong.fishisland.datasource.ai.BigFishMemoryDataSource;
import com.cong.fishisland.model.entity.chat.RoomMessage;
import com.cong.fishisland.model.ws.request.Message;
import com.cong.fishisland.model.ws.request.MessageWrapper;
import com.cong.fishisland.model.ws.response.BigFishSpeech;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import com.cong.fishisland.service.RoomMessageService;
import com.cong.fishisland.service.impl.chat.BigFishSettingsService;
import com.cong.fishisland.service.impl.chat.RobotChatMessageService;
import com.cong.fishisland.websocket.event.BigFishChatEvent;
import com.cong.fishisland.websocket.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import toolgood.words.StringSearch;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BigFishChatListenerTest {
    private static final String QUESTION_CONTEXT = "岛友昵称：\"合成小舟\"\n岛友说：\"@大肥鱼在吗\"\n对方引用：\"试着聊两句\"";
    private final BigFishDataSource deepSeek = mock(BigFishDataSource.class);
    private final BigFishMemoryDataSource memory = mock(BigFishMemoryDataSource.class);
    private final BigFishMemoryClassifier memoryClassifier = mock(BigFishMemoryClassifier.class);
    private final RedissonClient redis = mock(RedissonClient.class);
    private final WebSocketService socket = mock(WebSocketService.class);
    private final RoomMessageService roomMessages = mock(RoomMessageService.class);
    private final RobotChatMessageService robots = spy(new RobotChatMessageService(socket, roomMessages));
    private final BigFishSettingsService settings = mock(BigFishSettingsService.class);
    private final StringSearch words = mock(StringSearch.class);
    private final AtomicLong ticker = new AtomicLong();
    private final Deque<Runnable> memoryTasks = new ArrayDeque<>();
    private final BigFishChatListener listener = new BigFishChatListener(deepSeek, redis, socket, words, robots, settings,
            memory, memoryClassifier, Runnable::run, memoryTasks::addLast, ticker::get);
    private final BigFishChatEvent message = new BigFishChatEvent("test-id", 7L, "合成小舟", "@大肥鱼在吗", "试着聊两句");

    @BeforeEach
    void setUp() {
        when(settings.isEnabled()).thenReturn(true);
        when(deepSeek.isConfigured()).thenReturn(true);
        when(words.Replace(anyString())).thenAnswer(call -> call.getArgument(0));
        when(roomMessages.save(any(RoomMessage.class))).thenReturn(true);
        allow("chat:big-fish:source:test-id", 1, TimeUnit.DAYS, true);
    }

    @Test
    void directMentionSendsPrivateThinkingAndPersistsBeforeBroadcasting() {
        allow("chat:big-fish:mention:7", 5, TimeUnit.SECONDS, true);
        when(deepSeek.reply(QUESTION_CONTEXT, true)).thenReturn("小舟，来啦！\n[表情=开心]");

        listener.onChat(message);

        verify(robots).saveBigFishReply("小舟，来啦！", "开心");
        verify(socket).sendToUid(argThat(frame -> "thinking".equals(speech(frame).getStatus())), eq(7L));
        ArgumentCaptor<RoomMessage> saved = ArgumentCaptor.forClass(RoomMessage.class);
        InOrder order = inOrder(roomMessages, socket);
        order.verify(roomMessages).save(saved.capture());
        RoomMessage record = saved.getValue();
        Message persisted = JSON.parseObject(record.getMessageJson(), MessageWrapper.class).getMessage();
        assertEquals(-1L, record.getRoomId());
        assertEquals(-2L, record.getUserId());
        assertEquals(record.getMessageId(), persisted.getId());
        assertTrue(persisted.getBigFish());
        assertEquals("大肥鱼", persisted.getSender().getName());
        assertEquals("小舟，来啦！", persisted.getContent());
        order.verify(socket).sendToAllOnline(argThat(frame -> "bigFish".equals(frame.getType())
                && "test-id".equals(speech(frame).getId())
                && "speech".equals(speech(frame).getStatus())
                && persisted.equals(speech(frame).getMessage())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"肥鱼", "想跟大肥鱼聊两句"})
    void eitherKeywordStartsAReplyWithoutAtSign(String content) {
        allow("chat:big-fish:mention:7", 5, TimeUnit.SECONDS, true);
        when(deepSeek.reply(anyString(), eq(true))).thenReturn("来啦。");

        listener.onChat(new BigFishChatEvent("test-id", 7L, "合成小舟", content, null));

        verify(deepSeek).reply("岛友昵称：\"合成小舟\"\n岛友说：\"" + content + "\"", true);
        verify(socket).sendToAllOnline(any());
    }

    @Test
    void absentNicknameFallsBackToIslandFriend() {
        allow("chat:big-fish:mention:7", 5, TimeUnit.SECONDS, true);
        when(deepSeek.reply(anyString(), eq(true))).thenReturn("岛友，我在呢。 [表情=岛友昵称]");

        listener.onChat(new BigFishChatEvent("test-id", 7L, " \t", "肥鱼在吗", null));

        verify(deepSeek).reply("岛友昵称：\"岛友\"\n岛友说：\"肥鱼在吗\"", true);
        verify(robots).saveBigFishReply("岛友，我在呢。", null);
    }

    @Test
    void failedSaveDoesNotBroadcastTheReply() {
        allow("chat:big-fish:mention:7", 5, TimeUnit.SECONDS, true);
        when(deepSeek.reply(anyString(), eq(true))).thenReturn("听见啦。");
        when(roomMessages.save(any(RoomMessage.class))).thenReturn(false);
        when(memory.isConfigured()).thenReturn(true);

        listener.onChat(message);

        verify(socket).sendToUid(argThat(frame -> "error".equals(speech(frame).getStatus())), eq(7L));
        verify(socket, never()).sendToAllOnline(any());
        assertTrue(memoryTasks.isEmpty());
    }

    @Test
    void recallsForTheSenderAndWritesOnlyTheRequestedExcerptAfterBroadcast() {
        allow("chat:big-fish:mention:7", 5, TimeUnit.SECONDS, true);
        when(memory.isConfigured()).thenReturn(true);
        String preference = "我喜欢吃板烧鸡腿堡";
        String request = preference + "记住了吗大肥鱼";
        when(memory.recall(-1L, 7L, request)).thenReturn(Collections.singletonList("喜欢画帆船"));
        when(memoryClassifier.requestedMemories(Collections.emptyList(), Collections.singletonList(request)))
                .thenReturn(Collections.singletonList(preference));
        when(deepSeek.reply(startsWith("相关记忆片段：[\"喜欢画帆船\"]\n"), eq(true))).thenReturn("小舟，收到！");

        listener.onChat(new BigFishChatEvent("test-id", 7L, "合成小舟", request, "试着聊两句"));

        verify(memory, never()).remember(anyLong(), anyLong(), anyString(), anyList());
        assertEquals(1, memoryTasks.size());
        memoryTasks.removeFirst().run();
        InOrder order = inOrder(memory, memoryClassifier, deepSeek, roomMessages, socket);
        order.verify(memory).recall(-1L, 7L, request);
        order.verify(deepSeek).reply(startsWith("相关记忆片段：[\"喜欢画帆船\"]\n"), eq(true));
        order.verify(roomMessages).save(any(RoomMessage.class));
        order.verify(socket).sendToAllOnline(any());
        order.verify(memoryClassifier).requestedMemories(Collections.emptyList(), Collections.singletonList(request));
        order.verify(memory).remember(-1L, 7L, "test-id", Collections.singletonList(preference));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void negativeDecisionOrFailedClassificationSkipsMemoryWrite(boolean providerFailed) {
        allow("chat:big-fish:mention:7", 5, TimeUnit.SECONDS, true);
        when(memory.isConfigured()).thenReturn(true);
        when(deepSeek.reply(QUESTION_CONTEXT, true)).thenReturn("小舟，来啦！");
        if (providerFailed) when(memoryClassifier.requestedMemories(anyList(), anyList())).thenThrow(new IllegalStateException());

        listener.onChat(message);
        memoryTasks.removeFirst().run();

        verify(memoryClassifier).requestedMemories(Collections.emptyList(), Collections.singletonList("@大肥鱼在吗"));
        verify(memory, never()).remember(anyLong(), anyLong(), anyString(), anyList());
        verify(socket).sendToAllOnline(any());
    }

    @Test
    void resolvesAnExplicitFollowUpAgainstTheUserTextBeforeThatBatch() {
        allow("chat:big-fish:mention:7", 5, TimeUnit.SECONDS, true);
        allow("chat:big-fish:source:next", 1, TimeUnit.DAYS, true);
        when(memory.isConfigured()).thenReturn(true);
        when(deepSeek.reply(anyString(), eq(true))).thenReturn("小舟，收到！");
        String earlier = "大肥鱼，我在练习画帆船";
        String followUp = "帮我记住刚才说的";
        when(memoryClassifier.requestedMemories(Collections.singletonList(earlier), Collections.singletonList(followUp)))
                .thenReturn(Collections.singletonList("我在练习画帆船"));

        listener.onChat(new BigFishChatEvent("test-id", 7L, "合成小舟", earlier, null));
        listener.onChat(new BigFishChatEvent("next", 7L, "合成小舟", followUp, null));
        memoryTasks.removeFirst().run();
        verify(memoryClassifier).requestedMemories(Collections.emptyList(), Collections.singletonList(earlier));
        verify(memory, never()).remember(anyLong(), anyLong(), anyString(), anyList());
        memoryTasks.removeFirst().run();
        verify(memory).remember(-1L, 7L, "next", Collections.singletonList("我在练习画帆船"));
    }

    @Test
    void cooldownAndProviderFailureStayPrivate() {
        allow("chat:big-fish:mention:7", 5, TimeUnit.SECONDS, false);
        listener.onChat(message);
        verify(deepSeek, never()).reply(anyString(), anyBoolean());
        verify(socket).sendToUid(argThat(frame -> "error".equals(speech(frame).getStatus())), eq(7L));

        reset(socket);
        allow("chat:big-fish:mention:7", 5, TimeUnit.SECONDS, true);
        when(deepSeek.reply(anyString(), eq(true))).thenThrow(new IllegalStateException());
        listener.onChat(message);
        verify(socket).sendToUid(argThat(frame -> "error".equals(speech(frame).getStatus())), eq(7L));
        verify(socket, never()).sendToAllOnline(any());
    }

    @Test
    void duplicateSourcesAreIgnoredAndInterjectionsCanRemainSilent() {
        allow("chat:big-fish:source:test-id", 1, TimeUnit.DAYS, false);
        listener.onChat(message);
        verifyNoInteractions(socket);

        allow("chat:big-fish:source:test-id", 1, TimeUnit.DAYS, true);
        allow("chat:big-fish:interjection:-1", 5, TimeUnit.MINUTES, true);
        when(deepSeek.reply(anyString(), eq(false))).thenReturn("[NO_REPLY]");
        listener.interject(new BigFishChatEvent("test-id", 7L, "合成小舟", "今天适合摸鱼吗", null));
        verify(deepSeek).reply("岛友昵称：\"合成小舟\"\n岛友说：\"今天适合摸鱼吗\"", false);
        verifyNoInteractions(memory, memoryClassifier);
        verifyNoInteractions(socket);
        assertTrue(BigFishChatListener.canInterject("今天适合摸鱼吗"));
        assertFalse(BigFishChatListener.canInterject("[img]https://example.com/sample.png[/img]"));
        assertFalse(BigFishChatListener.canInterject("大家在吗 @摸鱼助手"));
    }

    @Test
    void followUpWindowRollsAtTenSecondsAndRetainsRecentConversation() {
        allow("chat:big-fish:mention:7", 5, TimeUnit.SECONDS, true);
        allow("chat:big-fish:source:next", 1, TimeUnit.DAYS, true);
        allow("chat:big-fish:source:boundary", 1, TimeUnit.DAYS, true);
        allow("chat:big-fish:source:expired", 1, TimeUnit.DAYS, true);
        when(deepSeek.reply(anyString(), eq(true))).thenReturn("这不是来了吗？", "你说说看。", "听见啦。");

        listener.onChat(message);
        ticker.addAndGet(TimeUnit.SECONDS.toNanos(7));
        listener.onChat(new BigFishChatEvent("next", 7L, "合成小帆", "等等，还有个事", null));
        listener.onChat(new BigFishChatEvent("other", 8L, "合成小鱼", "嗯", null));
        verify(deepSeek).reply(QUESTION_CONTEXT + "\n大肥鱼刚才说：这不是来了吗？"
                + "\n岛友昵称：\"合成小帆\"\n岛友说：\"等等，还有个事\"", true);
        ticker.addAndGet(TimeUnit.SECONDS.toNanos(10));
        listener.onChat(new BigFishChatEvent("boundary", 7L, "合成小帆", "还有呢", null));

        ticker.addAndGet(TimeUnit.SECONDS.toNanos(10) + 1);
        listener.onChat(new BigFishChatEvent("expired", 7L, "合成小帆", "嗯", null));
        verify(deepSeek, times(3)).reply(anyString(), eq(true));
        verify(socket, times(3)).sendToAllOnline(any());
    }

    @Test
    void messagesArrivingBeforeTheWorkerRunsAreCombinedAndDeduplicated() {
        Deque<Runnable> tasks = new ArrayDeque<>();
        BigFishChatListener queued = new BigFishChatListener(deepSeek, redis, socket, words, robots, settings,
                memory, memoryClassifier, tasks::addLast, memoryTasks::addLast, ticker::get);
        allow("chat:big-fish:mention:7", 5, TimeUnit.SECONDS, true);
        allow("chat:big-fish:source:next", 1, TimeUnit.DAYS, true);
        when(deepSeek.reply(anyString(), eq(true))).thenReturn("讲吧，我在听。");

        BigFishChatEvent next = new BigFishChatEvent("next", 7L, "合成小舟", "还有后半句", null);
        queued.onChat(message);
        queued.onChat(next);
        queued.onChat(next);
        assertEquals(1, tasks.size());
        tasks.removeFirst().run();

        verify(deepSeek).reply(QUESTION_CONTEXT + "\n岛友昵称：\"合成小舟\"\n岛友说：\"还有后半句\"", true);
        verify(deepSeek, times(1)).reply(anyString(), eq(true));
        verify(socket).sendToAllOnline(argThat(frame -> "next".equals(speech(frame).getId())));
    }

    @Test
    void disabledSettingsSilenceKeywordsFollowUpsAndInterjections() {
        allow("chat:big-fish:mention:7", 5, TimeUnit.SECONDS, true);
        when(deepSeek.reply(anyString(), eq(true))).thenReturn("来啦。");
        listener.onChat(message);
        verify(deepSeek).reply(QUESTION_CONTEXT, true);
        clearInvocations(deepSeek, socket, roomMessages);
        when(settings.isEnabled()).thenReturn(false);

        listener.onChat(new BigFishChatEvent("next", 7L, "合成小舟", "还有一件事", null));
        listener.onChat(message);
        listener.interject(new BigFishChatEvent("interjection", 8L, "合成小鱼", "今天适合摸鱼吗", null));

        verifyNoInteractions(deepSeek, socket, roomMessages);
    }

    @Test
    void disablingDuringGenerationClearsThinkingAndDiscardsTheReply() {
        allow("chat:big-fish:mention:7", 5, TimeUnit.SECONDS, true);
        when(settings.isEnabled()).thenReturn(true, false);
        when(deepSeek.reply(QUESTION_CONTEXT, true)).thenReturn("来啦。");

        listener.onChat(message);

        verify(socket).sendToUid(argThat(frame -> "thinking".equals(speech(frame).getStatus())), eq(7L));
        verify(socket).sendToUid(argThat(frame -> "error".equals(speech(frame).getStatus())
                && "大肥鱼已关闭。".equals(speech(frame).getText())), eq(7L));
        verify(roomMessages, never()).save(any(RoomMessage.class));
        verify(socket, never()).sendToAllOnline(any());
    }

    private BigFishSpeech speech(WSBaseResp<?> frame) {
        return (BigFishSpeech) frame.getData();
    }

    private void allow(String key, long duration, TimeUnit unit, boolean allowed) {
        @SuppressWarnings("unchecked")
        RBucket<Object> bucket = mock(RBucket.class);
        when(bucket.trySet("1", duration, unit)).thenReturn(allowed);
        when(redis.getBucket(key)).thenReturn(bucket);
    }
}
