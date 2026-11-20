package com.cong.fishisland.websocket.listener;

import com.alibaba.fastjson.JSON;
import com.cong.fishisland.config.ThreadPoolConfig;
import com.cong.fishisland.datasource.ai.BigFishDataSource;
import com.cong.fishisland.model.enums.MessageTypeEnum;
import com.cong.fishisland.model.ws.request.Message;
import com.cong.fishisland.model.ws.response.BigFishSpeech;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import com.cong.fishisland.service.impl.chat.BigFishSettingsService;
import com.cong.fishisland.service.impl.chat.RobotChatMessageService;
import com.cong.fishisland.websocket.event.BigFishChatEvent;
import com.cong.fishisland.websocket.service.WebSocketService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import toolgood.words.StringSearch;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
@Slf4j
public class BigFishChatListener {
    private static final String CALL_KEYWORD = "肥鱼";
    private static final Pattern SPEECH_CANDIDATE = Pattern.compile("^[^@＠/\\[<`].{5,399}$");
    private static final long FOLLOW_UP_WINDOW = TimeUnit.SECONDS.toNanos(10);
    private static final int MENTION_COOLDOWN_SECONDS = 5;
    private static final int INTERJECTION_CANDIDATE_PERCENT = 30;
    private static final int RECENT_LINES = 6;

    private final BigFishDataSource deepSeek;
    private final RedissonClient redis;
    private final WebSocketService socket;
    private final StringSearch words;
    private final Executor executor;
    private final RobotChatMessageService robotMessages;
    private final BigFishSettingsService settings;
    private final Ticker ticker;
    private final Cache<Long, Conversation> conversations;

    private static final class Conversation {
        final BigFishChatEvent start;
        final Deque<BigFishChatEvent> pending = new ArrayDeque<>();
        final Deque<String> recent = new ArrayDeque<>();
        final Deque<String> sources = new ArrayDeque<>();
        boolean running = true;
        long lastMessage;

        Conversation(BigFishChatEvent start, long now) {
            this.start = start;
            lastMessage = now;
            sources.addLast(start.getMessageId());
        }
    }

    @Autowired
    public BigFishChatListener(BigFishDataSource deepSeek, RedissonClient redis, WebSocketService socket,
                               StringSearch words, RobotChatMessageService robotMessages, BigFishSettingsService settings,
                               @Qualifier(ThreadPoolConfig.BIG_FISH_EXECUTOR) Executor executor) {
        this(deepSeek, redis, socket, words, robotMessages, settings, executor, Ticker.systemTicker());
    }

    BigFishChatListener(BigFishDataSource deepSeek, RedissonClient redis, WebSocketService socket,
                        StringSearch words, RobotChatMessageService robotMessages, BigFishSettingsService settings,
                        Executor executor, Ticker ticker) {
        this.deepSeek = deepSeek;
        this.redis = redis;
        this.socket = socket;
        this.words = words;
        this.robotMessages = robotMessages;
        this.settings = settings;
        this.executor = executor;
        this.ticker = ticker;
        this.conversations = Caffeine.newBuilder().ticker(ticker)
                .maximumSize(256).expireAfterAccess(1, TimeUnit.MINUTES).build();
    }

    @EventListener
    public void onChat(BigFishChatEvent event) {
        if (event.getUserId() <= 0 || !StringUtils.hasText(event.getContent())
                || !StringUtils.hasText(event.getMessageId()) || event.getMessageId().length() > 128) return;
        boolean called = event.getContent().contains(CALL_KEYWORD);
        if (continueConversation(event, called)) return;
        if (ThreadLocalRandom.current().nextInt(100) >= INTERJECTION_CANDIDATE_PERCENT
                || !canInterject(event.getContent())) return;
        try {
            executor.execute(() -> interject(event));
        } catch (RejectedExecutionException ignored) {
            // Scheduled interjections can yield when the reply queue is busy.
        }
    }

    private boolean continueConversation(BigFishChatEvent event, boolean called) {
        Conversation conversation;
        boolean first;
        synchronized (conversations) {
            long now = ticker.read();
            conversation = conversations.getIfPresent(event.getUserId());
            first = conversation == null || now - conversation.lastMessage > FOLLOW_UP_WINDOW;
            if (first) {
                if (!called) {
                    if (conversation != null) conversations.invalidate(event.getUserId());
                    return false;
                }
                conversation = new Conversation(event, now);
                conversations.put(event.getUserId(), conversation);
            } else {
                if (conversation.sources.contains(event.getMessageId())) return true;
                if (conversation.sources.size() == RECENT_LINES * 2) conversation.sources.removeFirst();
                conversation.sources.addLast(event.getMessageId());
                conversation.lastMessage = now;
                if (conversation.pending.size() == RECENT_LINES) conversation.pending.removeFirst();
                conversation.pending.addLast(event);
                if (conversation.running) return true;
                conversation.running = true;
            }
        }
        Conversation scheduled = conversation;
        try {
            executor.execute(() -> drain(scheduled, first));
        } catch (RejectedExecutionException ignored) {
            conversations.asMap().remove(event.getUserId(), scheduled);
            notifySender(event, "error", "大肥鱼有点忙，请稍后再试。");
        }
        return true;
    }

    private void drain(Conversation conversation, boolean first) {
        BigFishChatEvent trigger = conversation.start;
        try {
            while (true) {
                List<BigFishChatEvent> batch;
                synchronized (conversations) {
                    if (!first && conversation.pending.isEmpty()) {
                        conversation.running = false;
                        return;
                    }
                    batch = new ArrayList<>(conversation.pending);
                    conversation.pending.clear();
                }
                trigger = batch.isEmpty() ? conversation.start : batch.get(batch.size() - 1);
                if (!canSpeak(trigger, true)) {
                    conversations.asMap().remove(trigger.getUserId(), conversation);
                    return;
                }
                if (first) {
                    if (!reserve(conversation.start)) {
                        conversations.asMap().remove(trigger.getUserId(), conversation);
                        return;
                    }
                    if (!redis.getBucket("chat:big-fish:mention:" + trigger.getUserId())
                            .trySet("1", MENTION_COOLDOWN_SECONDS, TimeUnit.SECONDS)) {
                        conversations.asMap().remove(trigger.getUserId(), conversation);
                        notifySender(conversation.start, "error", "大肥鱼刚被叫过，等一会儿再试吧。");
                        return;
                    }
                }
                batch.removeIf(event -> !reserve(event));
                if (first) batch.add(0, conversation.start);
                first = false;
                if (batch.isEmpty()) continue;
                for (BigFishChatEvent event : batch) remember(conversation, contextLine(event));
                trigger = batch.get(batch.size() - 1);
                notifySender(trigger, "thinking", "");
                String answer = generate(trigger, true, String.join("\n", conversation.recent));
                if (answer != null) remember(conversation, "大肥鱼刚才说：" + answer);
            }
        } catch (RuntimeException failure) {
            conversations.asMap().remove(trigger.getUserId(), conversation);
            reportFailure(trigger, true, failure);
        }
    }

    private void remember(Conversation conversation, String line) {
        if (conversation.recent.size() == RECENT_LINES) conversation.recent.removeFirst();
        conversation.recent.addLast(line);
    }

    static boolean canInterject(String content) {
        return SPEECH_CANDIDATE.matcher(content.trim()).matches() && !content.contains("http")
                && !content.contains("@") && !content.contains("＠");
    }

    void interject(BigFishChatEvent event) {
        try {
            if (canSpeak(event, false)
                    && redis.getBucket("chat:big-fish:interjection:-1").trySet("1", 5, TimeUnit.MINUTES)
                    && reserve(event)) {
                generate(event, false, contextLine(event));
            }
        } catch (RuntimeException failure) {
            reportFailure(event, false, failure);
        }
    }

    private boolean canSpeak(BigFishChatEvent event, boolean reply) {
        if (!settings.isEnabled()) return false;
        if (deepSeek.isConfigured()) return true;
        if (reply) notifySender(event, "error", "大肥鱼暂时无法回复。");
        return false;
    }

    private String generate(BigFishChatEvent event, boolean reply, String chat) {
        try {
            String answer = deepSeek.reply(chat, reply);
            if (!settings.isEnabled()) {
                if (reply) notifySender(event, "error", "大肥鱼已关闭。");
                return null;
            }
            if (!StringUtils.hasText(answer) || "[NO_REPLY]".equalsIgnoreCase(answer.trim())) {
                if (reply) notifySender(event, "error", "大肥鱼没接上话，请再试一次。");
                return null;
            }
            String text = words.Replace(limit(answer.replaceAll("\\s+", " ").trim(), 240));
            Message message = robotMessages.saveBigFishReply(text);
            socket.sendToAllOnline(frame(event, "speech", text, message));
            return text;
        } catch (RuntimeException failure) {
            reportFailure(event, reply, failure);
            return null;
        }
    }

    private boolean reserve(BigFishChatEvent event) {
        return redis.getBucket("chat:big-fish:source:" + event.getMessageId()).trySet("1", 1, TimeUnit.DAYS);
    }

    private String contextLine(BigFishChatEvent event) {
        String name = StringUtils.hasText(event.getUserName()) ? limit(event.getUserName().trim(), 64) : "岛友";
        String chat = "岛友昵称：" + JSON.toJSONString(name)
                + "\n岛友说：" + JSON.toJSONString(limit(event.getContent(), 600));
        if (StringUtils.hasText(event.getQuotedContent())) {
            chat += "\n对方引用：" + JSON.toJSONString(limit(event.getQuotedContent(), 400));
        }
        return chat;
    }

    private void reportFailure(BigFishChatEvent event, boolean reply, RuntimeException failure) {
        log.warn("Big fish reply failed: {}", failure.getClass().getSimpleName());
        if (reply) notifySender(event, "error", "大肥鱼没能回复，请稍后再试。");
    }

    private String limit(String text, int length) {
        return text.substring(0, text.offsetByCodePoints(0, Math.min(length, text.codePointCount(0, text.length()))));
    }

    private void notifySender(BigFishChatEvent event, String status, String text) {
        socket.sendToUid(frame(event, status, text, null), event.getUserId());
    }

    private WSBaseResp<?> frame(BigFishChatEvent event, String status, String text, Message message) {
        return WSBaseResp.builder().type(MessageTypeEnum.BIG_FISH.getType())
                .data(new BigFishSpeech(event.getMessageId(), status, text, message)).build();
    }
}
