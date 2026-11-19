package com.cong.fishisland.service.impl.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.mapper.user.UserChatBubbleMapper;
import com.cong.fishisland.model.entity.user.ChatBubbleProduct;
import com.cong.fishisland.model.entity.user.UserChatBubble;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.model.enums.user.PointsRecordSourceEnum;
import com.cong.fishisland.model.vo.user.ChatBubbleVO;
import com.cong.fishisland.service.UserPointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatBubbleService {
    private static final int VALID_DAYS = 30;

    private final UserChatBubbleMapper mapper;
    private final UserPointsService points;
    private final PlatformTransactionManager transactionManager;
    private final ChatBubbleCatalog catalog;

    public List<ChatBubbleVO> list(long userId) {
        Map<String, UserChatBubble> owned = mapper.findAll(userId).stream()
                .collect(Collectors.toMap(UserChatBubble::getBubbleId, Function.identity()));
        long now = System.currentTimeMillis();
        return catalog.list().stream().map(style -> {
            UserChatBubble grant = owned.get(style.getId());
            String expiresAt = grant == null ? null : Instant.ofEpochMilli(grant.getExpiresAt()).toString();
            boolean equipped = grant != null && grant.getExpiresAt() > now && Integer.valueOf(1).equals(grant.getEquipped());
            return new ChatBubbleVO(style.getId(), style.getName(), style.getPoints(), VALID_DAYS,
                    expiresAt, equipped, style.artwork());
        }).collect(Collectors.toList());
    }

    public void exchange(long userId, String bubbleId, Integer expectedPoints) {
        ChatBubbleCatalog.requireId(bubbleId);
        change(userId, () -> {
            // Serialize debits in the database as well, even if the Redis lease expires.
            UserPoints lockedAccount = points.getOne(new LambdaQueryWrapper<UserPoints>()
                    .select(UserPoints::getUserId).eq(UserPoints::getUserId, userId).last("FOR UPDATE"));
            ThrowUtils.throwIf(lockedAccount == null, ErrorCode.NOT_FOUND_ERROR, "积分信息不存在");
            ChatBubbleProduct style = catalog.forExchange(bubbleId);
            int price = style.getPoints();
            ThrowUtils.throwIf(expectedPoints == null || expectedPoints != price, ErrorCode.OPERATION_ERROR,
                    "气泡价格已调整，请重新读取商城后再确认兑换");
            Long previousExpiry = mapper.findExpiry(userId, style.getId());
            ThrowUtils.throwIf(previousExpiry != null && previousExpiry > System.currentTimeMillis(),
                    ErrorCode.OPERATION_ERROR, "该气泡仍在有效期内");
            points.checkAvailablePoints(userId, price);
            points.updateUsedPoints(userId, price, PointsRecordSourceEnum.CHAT_BUBBLE_EXCHANGE.getValue(),
                    style.getId(), "兑换聊天气泡「" + style.getName() + "」");
            long expiresAt = Instant.now().plus(Duration.ofDays(VALID_DAYS)).toEpochMilli();
            ThrowUtils.throwIf(mapper.grant(userId, style.getId(), expiresAt) < 1,
                    ErrorCode.OPERATION_ERROR, "保存气泡权益失败，请重试");
        });
    }

    public void equip(long userId, String bubbleId) {
        if (bubbleId != null) ChatBubbleCatalog.requireId(bubbleId);
        change(userId, () -> {
            mapper.clearSelection(userId);
            if (bubbleId != null) {
                ThrowUtils.throwIf(mapper.select(userId, bubbleId, System.currentTimeMillis()) != 1,
                        ErrorCode.OPERATION_ERROR, "气泡未兑换或已过期");
            }
        });
    }

    public ChatBubbleProduct activeStyle(long userId) {
        return mapper.findActive(userId, System.currentTimeMillis());
    }

    private void change(long userId, Runnable action) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        points.runWithUserPointsLocks(new Long[]{userId}, () -> transaction.execute(status -> {
            action.run();
            return null;
        }));
    }
}
