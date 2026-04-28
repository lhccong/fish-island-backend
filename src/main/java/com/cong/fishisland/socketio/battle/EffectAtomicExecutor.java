package com.cong.fishisland.socketio.battle;

import com.cong.fishisland.model.fishbattle.battle.BattleChampionState;
import com.cong.fishisland.model.fishbattle.battle.BattleRoom;
import com.cong.fishisland.model.fishbattle.battle.BattleVector3;
import org.springframework.stereotype.Service;

/**
 * 效果原子执行器（main 最小版）。
 * 当前仅实现召唤师技能真正生效所需的治疗、状态、伤害、传送。
 */
@Service
public class EffectAtomicExecutor {
    private final StatusEffectService statusEffectService;
    private final Battle3dRoomManager battleRoomManager;

    public EffectAtomicExecutor(StatusEffectService statusEffectService, Battle3dRoomManager battleRoomManager) {
        this.statusEffectService = statusEffectService;
        this.battleRoomManager = battleRoomManager;
    }

    public void applyDamage(String sourceEntityId, String castInstanceId, String skillId, String slot,
                            BattleChampionState target, double amount, String damageType) {
        if (target == null || target.getHp() == null) {
            return;
        }
        double damage = Math.max(0D, amount);
        double nextHp = Math.max(0D, target.getHp() - damage);
        target.setHp(nextHp);
        if (nextHp <= 0D) {
            target.setDead(Boolean.TRUE);
            target.setIsDead(Boolean.TRUE);
            target.setAnimationState("death");
            target.setMoveTarget(null);
        }
    }

    public void applyHeal(String sourceEntityId, String castInstanceId, String skillId, String slot,
                          BattleChampionState target, double amount) {
        if (target == null || target.getHp() == null || target.getMaxHp() == null) {
            return;
        }
        double actualHeal = Math.max(0D, amount);
        target.setHp(Math.min(target.getMaxHp(), target.getHp() + actualHeal));
    }

    public void applyStatus(String castInstanceId, String skillId, String slot, String statusId,
                            String sourceEntityId, BattleChampionState target, int stacks, long durationMs) {
        if (target == null) {
            return;
        }
        BattleRoom room = battleRoomManager.findRoomByChampionId(target.getId());
        statusEffectService.apply(room != null ? room.getRoomId() : null, statusId, sourceEntityId, target.getId(), stacks, durationMs);
    }

    public void removeStatus(String castInstanceId, String skillId, String slot,
                             String statusId, BattleChampionState target, boolean removeAllStacks) {
        if (target == null) {
            return;
        }
        BattleRoom room = battleRoomManager.findRoomByChampionId(target.getId());
        statusEffectService.remove(room != null ? room.getRoomId() : null, target.getId(), statusId, removeAllStacks);
    }

    public void applyTeleport(BattleChampionState target, BattleVector3 destination) {
        if (target == null || target.getPosition() == null || destination == null) {
            return;
        }
        target.getPosition().setX(destination.getX());
        target.getPosition().setY(destination.getY());
        target.getPosition().setZ(destination.getZ());
        target.setMoveTarget(null);
        target.setInputMode("idle");
        target.setAnimationState("idle");
        target.setIdleStartedAt(System.currentTimeMillis());
    }
}
