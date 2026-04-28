package com.cong.fishisland.socketio.battle;

import com.cong.fishisland.model.fishbattle.battle.BattleChampionState;
import com.cong.fishisland.model.fishbattle.battle.BattleRoom;
import com.cong.fishisland.model.fishbattle.battle.BattleStructureState;
import com.cong.fishisland.model.fishbattle.battle.BattleVector3;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 效果原子执行器（main 最小版）。
 * 当前仅实现召唤师技能真正生效所需的治疗、状态、伤害、传送。
 */
@Service
public class EffectAtomicExecutor {
    private final StatusEffectService statusEffectService;
    private final Battle3dRoomManager battleRoomManager;
    private final Battle3dBroadcastService battleBroadcastService;

    public EffectAtomicExecutor(StatusEffectService statusEffectService, Battle3dRoomManager battleRoomManager,
                                Battle3dBroadcastService battleBroadcastService) {
        this.statusEffectService = statusEffectService;
        this.battleRoomManager = battleRoomManager;
        this.battleBroadcastService = battleBroadcastService;
    }

    public void applyDamage(String sourceEntityId, String castInstanceId, String skillId, String slot,
                            BattleChampionState target, double amount, String damageType) {
        if (target == null || target.getHp() == null) {
            return;
        }
        double rawDamage = Math.max(0D, amount);
        double actualDamage = applyResistance(rawDamage, damageType,
                target.getBaseArmor() != null ? target.getBaseArmor() : 0D,
                target.getBaseMr() != null ? target.getBaseMr() : 0D);
        double absorbedByShield = 0D;
        double shield = target.getShield() != null ? target.getShield() : 0D;
        if (shield > 0D) {
            if (shield >= actualDamage) {
                target.setShield(shield - actualDamage);
                absorbedByShield = actualDamage;
                broadcastDamageApplied(sourceEntityId, castInstanceId, skillId, slot, target,
                        actualDamage, absorbedByShield, target.getShield() != null ? target.getShield() : 0D, false);
                broadcastShieldChanged(sourceEntityId, castInstanceId, skillId, slot, target,
                        -absorbedByShield, target.getShield() != null ? target.getShield() : 0D);
                return;
            }
            absorbedByShield = shield;
            actualDamage -= shield;
            target.setShield(0D);
            broadcastShieldChanged(sourceEntityId, castInstanceId, skillId, slot, target, -absorbedByShield, 0D);
        }
        double nextHp = Math.max(0D, target.getHp() - actualDamage);
        target.setHp(nextHp);
        target.setIsDead(nextHp <= 0D);
        if (nextHp <= 0D) {
            target.setDead(Boolean.TRUE);
            target.setIsDead(Boolean.TRUE);
            target.setAnimationState("death");
            target.setMoveTarget(null);
        }
        if (sourceEntityId != null && target.getTeam() != null
                && (sourceEntityId.startsWith("blue_") || sourceEntityId.startsWith("red_"))
                && !sourceEntityId.equals(target.getId())) {
            BattleRoom room = battleRoomManager.findRoomByChampionId(sourceEntityId);
            if (room != null) {
                battleRoomManager.findChampion(room, sourceEntityId).ifPresent(attacker -> {
                    if (!target.getTeam().equals(attacker.getTeam())) {
                        attacker.setLastAttackedEnemyChampionAt(System.currentTimeMillis());
                    }
                });
            }
        }
        broadcastDamageApplied(sourceEntityId, castInstanceId, skillId, slot, target,
                actualDamage + absorbedByShield, absorbedByShield, target.getShield() != null ? target.getShield() : 0D,
                nextHp <= 0D);
        if (nextHp <= 0D) {
            broadcastDeathOccurred(sourceEntityId, castInstanceId, skillId, slot, target);
        }
    }

    public void applyHeal(String sourceEntityId, String castInstanceId, String skillId, String slot,
                          BattleChampionState target, double amount) {
        if (target == null || target.getHp() == null || target.getMaxHp() == null) {
            return;
        }
        double actualHeal = Math.max(0D, amount);
        double oldHp = target.getHp();
        target.setHp(Math.min(target.getMaxHp(), oldHp + actualHeal));
        double finalHeal = target.getHp() - oldHp;
        if (finalHeal > 0D) {
            broadcastHealApplied(sourceEntityId, castInstanceId, skillId, slot, target, finalHeal);
        }
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

    public void applyDamageToStructure(String sourceEntityId, String castInstanceId, String skillId, String slot,
                                        BattleStructureState target, double amount) {
        if (target == null || target.getHp() == null || Boolean.TRUE.equals(target.getIsDestroyed())) {
            return;
        }
        double armor = target.getArmor() != null ? target.getArmor() : 0D;
        double reduction = armor / (armor + 100D);
        double effectiveDmg = Math.max(0D, amount * (1D - reduction));
        double nextHp = Math.max(0D, target.getHp() - effectiveDmg);
        target.setHp(nextHp);
        if (nextHp <= 0D) {
            target.setIsDestroyed(Boolean.TRUE);
        }
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

    private double applyResistance(double rawDamage, String damageType, double armor, double mr) {
        if ("true".equalsIgnoreCase(damageType)) {
            return rawDamage;
        }
        double resistance = "magical".equalsIgnoreCase(damageType) ? mr : armor;
        if (resistance <= 0D) {
            return rawDamage;
        }
        return rawDamage * 100D / (100D + resistance);
    }

    private void broadcastDamageApplied(String sourceEntityId, String castInstanceId, String skillId, String slot,
                                        BattleChampionState target, double totalDamage, double absorbedByShield,
                                        double remainingShield, boolean targetDied) {
        if (target == null) {
            return;
        }
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        fields.put("castInstanceId", castInstanceId);
        fields.put("skillId", skillId);
        fields.put("slot", slot);
        fields.put("sourceEntityId", sourceEntityId);
        fields.put("targetEntityId", target.getId());
        fields.put("amount", totalDamage);
        fields.put("absorbedByShield", absorbedByShield);
        fields.put("remainingShield", remainingShield);
        fields.put("currentHp", target.getHp());
        fields.put("targetDied", targetDied);
        fields.put("position", target.getPosition());
        broadcastCombatEvent("damage-applied", "DamageApplied", fields);
    }

    private void broadcastHealApplied(String sourceEntityId, String castInstanceId, String skillId, String slot,
                                      BattleChampionState target, double amount) {
        if (target == null) {
            return;
        }
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        fields.put("castInstanceId", castInstanceId);
        fields.put("skillId", skillId);
        fields.put("slot", slot);
        fields.put("sourceEntityId", sourceEntityId);
        fields.put("targetEntityId", target.getId());
        fields.put("amount", amount);
        fields.put("currentHp", target.getHp());
        fields.put("position", target.getPosition());
        broadcastCombatEvent("heal-applied", "HealApplied", fields);
    }

    private void broadcastShieldChanged(String sourceEntityId, String castInstanceId, String skillId, String slot,
                                        BattleChampionState target, double delta, double currentShield) {
        if (target == null) {
            return;
        }
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        fields.put("castInstanceId", castInstanceId);
        fields.put("skillId", skillId);
        fields.put("slot", slot);
        fields.put("sourceEntityId", sourceEntityId);
        fields.put("targetEntityId", target.getId());
        fields.put("delta", delta);
        fields.put("currentShield", currentShield);
        fields.put("position", target.getPosition());
        broadcastCombatEvent("shield-changed", "ShieldChanged", fields);
    }

    private void broadcastDeathOccurred(String sourceEntityId, String castInstanceId, String skillId, String slot,
                                        BattleChampionState target) {
        if (target == null) {
            return;
        }
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        fields.put("castInstanceId", castInstanceId);
        fields.put("skillId", skillId);
        fields.put("slot", slot);
        fields.put("sourceEntityId", sourceEntityId);
        fields.put("targetEntityId", target.getId());
        fields.put("position", target.getPosition());
        broadcastCombatEvent("death-occurred", "DeathOccurred", fields);
    }

    private void broadcastCombatEvent(String eventIdPrefix, String eventType, Map<String, Object> fields) {
        BattleRoom room = resolveRoomForCombatEvent(fields);
        if (room == null) {
            return;
        }
        battleBroadcastService.broadcastCombatEvent(room, eventType, eventIdPrefix, System.currentTimeMillis(), fields);
    }

    private BattleRoom resolveRoomForCombatEvent(Map<String, Object> fields) {
        if (fields == null) {
            return null;
        }
        Object targetEntityId = fields.get("targetEntityId");
        if (targetEntityId instanceof String) {
            BattleRoom room = battleRoomManager.findRoomByChampionId((String) targetEntityId);
            if (room != null) {
                return room;
            }
        }
        Object sourceEntityId = fields.get("sourceEntityId");
        if (sourceEntityId instanceof String) {
            return battleRoomManager.findRoomByChampionId((String) sourceEntityId);
        }
        return null;
    }
}
