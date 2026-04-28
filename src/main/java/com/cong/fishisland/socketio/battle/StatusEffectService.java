package com.cong.fishisland.socketio.battle;

import com.cong.fishisland.model.fishbattle.battle.StatusEffectInstance;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Buff / Debuff 运行时服务。
 */
@Service
public class StatusEffectService {
    private final List<StatusEffectInstance> activeStatusEffects = new CopyOnWriteArrayList<StatusEffectInstance>();

    public StatusEffectInstance apply(String roomId, String statusId, String sourceEntityId, String targetEntityId, int stacks, long durationMs) {
        long now = System.currentTimeMillis();
        StatusEffectInstance instance = StatusEffectInstance.builder()
                .statusInstanceId("status-" + UUID.randomUUID())
                .roomId(roomId)
                .statusId(statusId)
                .sourceEntityId(sourceEntityId)
                .targetEntityId(targetEntityId)
                .stacks(stacks)
                .createdAt(now)
                .expiresAt(durationMs > 0 ? now + durationMs : Long.MAX_VALUE)
                .build();
        activeStatusEffects.add(instance);
        return instance;
    }

    public List<StatusEffectInstance> cleanupExpired(String roomId, long now) {
        List<StatusEffectInstance> expiredStatuses = new ArrayList<StatusEffectInstance>();
        for (StatusEffectInstance status : activeStatusEffects) {
            if ((roomId == null || roomId.equals(status.getRoomId()))
                    && status.getExpiresAt() != null && status.getExpiresAt() <= now) {
                expiredStatuses.add(status);
            }
        }
        if (!expiredStatuses.isEmpty()) {
            activeStatusEffects.removeAll(expiredStatuses);
        }
        return expiredStatuses;
    }

    public Optional<StatusEffectInstance> findActiveOnTarget(String roomId, String targetEntityId, String statusId) {
        return activeStatusEffects.stream()
                .filter(item -> roomId != null && roomId.equals(item.getRoomId()))
                .filter(item -> targetEntityId != null && targetEntityId.equals(item.getTargetEntityId()))
                .filter(item -> statusId != null && statusId.equals(item.getStatusId()))
                .findFirst();
    }

    public boolean hasStatus(String roomId, String targetEntityId, String statusId) {
        return findActiveOnTarget(roomId, targetEntityId, statusId).isPresent();
    }

    public List<StatusEffectInstance> remove(String roomId, String targetEntityId, String statusId, boolean removeAllStacks) {
        List<StatusEffectInstance> removedStatuses = new ArrayList<StatusEffectInstance>();
        for (StatusEffectInstance item : activeStatusEffects) {
            if (roomId == null || targetEntityId == null || statusId == null) {
                continue;
            }
            if (!roomId.equals(item.getRoomId()) || !targetEntityId.equals(item.getTargetEntityId()) || !statusId.equals(item.getStatusId())) {
                continue;
            }
            if (removeAllStacks || item.getStacks() == null || item.getStacks() <= 1) {
                removedStatuses.add(item);
                continue;
            }
            item.setStacks(item.getStacks() - 1);
        }
        if (!removedStatuses.isEmpty()) {
            activeStatusEffects.removeAll(removedStatuses);
        }
        return removedStatuses;
    }

    public List<StatusEffectInstance> getActiveStatusEffects(String roomId) {
        List<StatusEffectInstance> results = new ArrayList<StatusEffectInstance>();
        for (StatusEffectInstance item : activeStatusEffects) {
            if (roomId != null && roomId.equals(item.getRoomId())) {
                results.add(item);
            }
        }
        return results;
    }

    public void cleanupRoom(String roomId) {
        if (roomId == null) {
            return;
        }
        activeStatusEffects.removeIf(item -> roomId.equals(item.getRoomId()));
    }
}
