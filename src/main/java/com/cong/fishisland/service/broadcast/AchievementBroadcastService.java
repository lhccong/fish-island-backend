package com.cong.fishisland.service.broadcast;

import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.enums.pet.EquipSlotEnum;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.admin.BroadcastService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.function.Supplier;

/** 在线成就播报：业务提交后尽力发送，不影响已完成的结算。 */
@Service
@Slf4j
public class AchievementBroadcastService {
    private final BroadcastService broadcasts;
    private final UserService users;

    public AchievementBroadcastService(@Lazy BroadcastService broadcasts, @Lazy UserService users) {
        this.broadcasts = broadcasts;
        this.users = users;
    }

    public void treasureFound(Long userId, int points) {
        if (points == 10) {
            publishAfterCommit(() -> "恭喜「" + name(userId) + "」在营地挖宝获得 10 积分！");
        }
    }

    public void towerCleared(Long userId, int floor) {
        if (floor > 30) {
            publishAfterCommit(() -> "恭喜「" + name(userId) + "」通关爬塔第 " + floor + " 层！");
        }
    }

    public void equipmentUpgraded(Long userId, EquipSlotEnum slot, int level) {
        if (level > 10) {
            publishAfterCommit(() -> "恭喜「" + name(userId) + "」的" + slot.getLabel()
                    + "成功强化至 " + level + " 级！");
        }
    }

    public void redEntriesForged(Long userId, EquipSlotEnum slot, int newRedCount) {
        if (newRedCount > 0) {
            publishAfterCommit(() -> "恭喜「" + name(userId) + "」为" + slot.getLabel()
                    + "锻造出 " + newRedCount + " 条红词条！");
        }
    }

    public void sponsorReceived(Long userId, BigDecimal amount) {
        if (amount != null && amount.signum() > 0) {
            publishAfterCommit(() -> "感谢「" + name(userId) + "」赞助摸鱼岛 "
                    + amount.stripTrailingZeros().toPlainString() + " 元！感谢支持！");
        }
    }

    private String name(Long userId) {
        User user = users.getById(userId);
        String name = user == null ? null : user.getUserName();
        if (name == null) return "岛民";
        name = name.replaceAll("[\\p{Cntrl}\\p{Z}\\s]+", " ").trim();
        if (name.isEmpty()) return "岛民";
        int length = name.codePointCount(0, name.length());
        return length > 40 ? name.substring(0, name.offsetByCodePoints(0, 40)) : name;
    }

    private void publishAfterCommit(Supplier<String> content) {
        Runnable dispatch = () -> {
            try {
                broadcasts.publish(content.get());
            } catch (Exception ignored) {
                // 不记录昵称、订单或异常载荷，不让通知故障改变业务结果。
                log.warn("自动全体播报发送失败，已跳过");
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
        } else {
            dispatch.run();
        }
    }
}
