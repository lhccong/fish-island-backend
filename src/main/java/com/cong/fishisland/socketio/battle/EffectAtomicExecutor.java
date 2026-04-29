package com.cong.fishisland.socketio.battle;

import com.cong.fishisland.model.fishbattle.battle.BattleChampionState;
import com.cong.fishisland.model.fishbattle.battle.BattleMinionState;
import com.cong.fishisland.model.fishbattle.battle.BattleRoom;
import com.cong.fishisland.model.fishbattle.battle.BattleStructureState;
import com.cong.fishisland.model.fishbattle.battle.BattleVector3;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 效果原子执行器（main 最小版）。
 * 当前仅实现召唤师技能真正生效所需的治疗、状态、伤害、传送。
 */
@Service
public class EffectAtomicExecutor {
    private static final long ASSIST_WINDOW_MS = 10_000L;

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
        long now = System.currentTimeMillis();
        BattleRoom room = battleRoomManager.findRoomByEntityId(target.getId());
        BattleChampionState attackerChampion = resolveChampionAttacker(room, sourceEntityId);
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
                target.setDamageTaken((target.getDamageTaken() != null ? target.getDamageTaken() : 0D) + actualDamage);
                if (attackerChampion != null && target.getTeam() != null && attackerChampion.getTeam() != null
                        && !target.getTeam().equals(attackerChampion.getTeam())) {
                    attackerChampion.setDamageDealt((attackerChampion.getDamageDealt() != null ? attackerChampion.getDamageDealt() : 0D) + actualDamage);
                    attackerChampion.setLastAttackedEnemyChampionAt(now);
                    target.setLastDamagedByChampionId(attackerChampion.getId());
                    target.setLastDamagedAt(now);
                    if (target.getRecentDamageByChampion() == null) {
                        target.setRecentDamageByChampion(new LinkedHashMap<String, Long>());
                    }
                    target.getRecentDamageByChampion().put(attackerChampion.getId(), now);
                }
                broadcastShieldChanged(sourceEntityId, castInstanceId, skillId, slot, target, -absorbedByShield, target.getShield() != null ? target.getShield() : 0D);
                broadcastDamageApplied(sourceEntityId, castInstanceId, skillId, slot, target,
                        actualDamage, absorbedByShield, target.getShield() != null ? target.getShield() : 0D, false);
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
        target.setDamageTaken((target.getDamageTaken() != null ? target.getDamageTaken() : 0D) + actualDamage + absorbedByShield);
        if (attackerChampion != null && target.getTeam() != null && attackerChampion.getTeam() != null
                && !target.getTeam().equals(attackerChampion.getTeam())) {
            attackerChampion.setDamageDealt((attackerChampion.getDamageDealt() != null ? attackerChampion.getDamageDealt() : 0D) + actualDamage + absorbedByShield);
            attackerChampion.setLastAttackedEnemyChampionAt(now);
            target.setLastDamagedByChampionId(attackerChampion.getId());
            target.setLastDamagedAt(now);
            if (target.getRecentDamageByChampion() == null) {
                target.setRecentDamageByChampion(new LinkedHashMap<String, Long>());
            }
            target.getRecentDamageByChampion().put(attackerChampion.getId(), now);
        }
        if (nextHp <= 0D) {
            target.setDead(Boolean.TRUE);
            target.setIsDead(Boolean.TRUE);
            long respawnDurationMs = resolveChampionRespawnDurationMs(target);
            target.setDeadAt(now);
            target.setRespawnAt(now + respawnDurationMs);
            target.setRespawnDurationMs(respawnDurationMs);
            target.setRespawnTimer(respawnDurationMs / 1000D);
            target.setAnimationState("death");
            target.setMoveTarget(null);
            target.setAttackMoveTarget(null);
            target.setCurrentAttackTargetId(null);
            target.setCurrentAttackTargetType(null);
        }
        broadcastDamageApplied(sourceEntityId, castInstanceId, skillId, slot, target,
                actualDamage + absorbedByShield, absorbedByShield, target.getShield() != null ? target.getShield() : 0D,
                nextHp <= 0D);
        if (nextHp <= 0D) {
            handleChampionDeath(room, sourceEntityId, castInstanceId, skillId, slot, target, now, attackerChampion);
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

    public void applyDamageToMinion(String sourceEntityId, String castInstanceId, String skillId, String slot,
                                    BattleMinionState target, double amount, String damageType) {
        if (target == null || target.getHp() == null || Boolean.TRUE.equals(target.getDead())) {
            return;
        }
        double actualDamage = Math.max(0D, amount);
        double nextHp = Math.max(0D, target.getHp() - actualDamage);
        target.setHp(nextHp);
        boolean targetDied = nextHp <= 0D;
        if (targetDied) {
            target.setDead(Boolean.TRUE);
            target.setDeadAt(System.currentTimeMillis());
            target.setAnimationState("death");
        }
        broadcastDamageApplied(sourceEntityId, castInstanceId, skillId, slot, target.getId(), "minion",
                target.getPosition(), actualDamage, 0D, 0D, target.getHp(), targetDied);
        if (targetDied) {
            broadcastDeathOccurred(sourceEntityId, castInstanceId, skillId, slot, target.getId(), "minion", target.getPosition());
        }
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
        broadcastDamageApplied(sourceEntityId, castInstanceId, skillId, slot, target.getId(), "structure",
                target.getPosition(), effectiveDmg, 0D, 0D, target.getHp(), nextHp <= 0D);
        if (nextHp <= 0D) {
            target.setIsDestroyed(Boolean.TRUE);
            broadcastDeathOccurred(sourceEntityId, castInstanceId, skillId, slot, target.getId(), "structure", target.getPosition());
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
        broadcastDamageApplied(sourceEntityId, castInstanceId, skillId, slot, target.getId(), "champion",
                target.getPosition(), totalDamage, absorbedByShield, remainingShield, target.getHp(), targetDied);
    }

    private void broadcastDamageApplied(String sourceEntityId, String castInstanceId, String skillId, String slot,
                                        String targetEntityId, String targetType, BattleVector3 position,
                                        double totalDamage, double absorbedByShield, double remainingShield,
                                        Double currentHp, boolean targetDied) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        fields.put("castInstanceId", castInstanceId);
        fields.put("skillId", skillId);
        fields.put("slot", slot);
        fields.put("sourceEntityId", sourceEntityId);
        fields.put("targetEntityId", targetEntityId);
        fields.put("targetType", targetType);
        fields.put("amount", totalDamage);
        fields.put("absorbedByShield", absorbedByShield);
        fields.put("remainingShield", remainingShield);
        fields.put("currentHp", currentHp);
        fields.put("targetDied", targetDied);
        fields.put("position", position);
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

    private void handleChampionDeath(BattleRoom room, String sourceEntityId, String castInstanceId, String skillId, String slot,
                                     BattleChampionState target, long now, BattleChampionState attackerChampion) {
        if (target == null) {
            return;
        }
        target.setDeaths((target.getDeaths() != null ? target.getDeaths() : 0) + 1);
        String killerId = null;
        String killerTeam = null;
        if (attackerChampion != null && target.getTeam() != null && attackerChampion.getTeam() != null
                && !target.getTeam().equals(attackerChampion.getTeam())) {
            attackerChampion.setKills((attackerChampion.getKills() != null ? attackerChampion.getKills() : 0) + 1);
            killerId = attackerChampion.getId();
            killerTeam = attackerChampion.getTeam();
            if (room != null) {
                if ("blue".equals(attackerChampion.getTeam())) {
                    room.setBlueKills((room.getBlueKills() != null ? room.getBlueKills() : 0) + 1);
                } else if ("red".equals(attackerChampion.getTeam())) {
                    room.setRedKills((room.getRedKills() != null ? room.getRedKills() : 0) + 1);
                }
            }
        }

        List<String> assistIds = new java.util.ArrayList<String>();
        if (room != null && target.getRecentDamageByChampion() != null && !target.getRecentDamageByChampion().isEmpty()) {
            for (Map.Entry<String, Long> entry : new LinkedHashMap<String, Long>(target.getRecentDamageByChampion()).entrySet()) {
                String contributorId = entry.getKey();
                Long damagedAt = entry.getValue();
                if (contributorId == null || damagedAt == null || now - damagedAt > ASSIST_WINDOW_MS) {
                    continue;
                }
                if (killerId != null && killerId.equals(contributorId)) {
                    continue;
                }
                BattleChampionState contributor = battleRoomManager.findChampion(room, contributorId).orElse(null);
                if (contributor == null || Boolean.TRUE.equals(contributor.getDead())) {
                    continue;
                }
                if (target.getTeam() != null && contributor.getTeam() != null && target.getTeam().equals(contributor.getTeam())) {
                    continue;
                }
                contributor.setAssists((contributor.getAssists() != null ? contributor.getAssists() : 0) + 1);
                assistIds.add(contributorId);
            }
        }
        if (target.getRecentDamageByChampion() != null) {
            target.getRecentDamageByChampion().clear();
        }
        broadcastDeathOccurred(sourceEntityId, castInstanceId, skillId, slot, target, killerId, killerTeam, assistIds);
    }

    private BattleChampionState resolveChampionAttacker(BattleRoom room, String sourceEntityId) {
        if (room == null || sourceEntityId == null || sourceEntityId.trim().isEmpty()) {
            return null;
        }
        return battleRoomManager.findChampion(room, sourceEntityId).orElse(null);
    }

    private long resolveChampionRespawnDurationMs(BattleChampionState target) {
        int level = target != null && target.getLevel() != null ? target.getLevel() : 1;
        return Math.max(8000L, Math.min(45000L, (8L + Math.max(0, level - 1L) * 2L) * 1000L));
    }

    private void broadcastDeathOccurred(String sourceEntityId, String castInstanceId, String skillId, String slot,
                                        BattleChampionState target, String killerId, String killerTeam, List<String> assistIds) {
        if (target == null) {
            return;
        }
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        fields.put("castInstanceId", castInstanceId);
        fields.put("skillId", skillId);
        fields.put("slot", slot);
        fields.put("sourceEntityId", sourceEntityId);
        fields.put("targetEntityId", target.getId());
        fields.put("targetType", "champion");
        fields.put("position", target.getPosition());
        fields.put("killerId", killerId);
        fields.put("killerTeam", killerTeam);
        fields.put("victimTeam", target.getTeam());
        fields.put("assistIds", assistIds);
        fields.put("respawnAt", target.getRespawnAt());
        broadcastCombatEvent("death-occurred", "DeathOccurred", fields);
    }

    private void broadcastDeathOccurred(String sourceEntityId, String castInstanceId, String skillId, String slot,
                                        String targetEntityId, String targetType, BattleVector3 position) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        fields.put("castInstanceId", castInstanceId);
        fields.put("skillId", skillId);
        fields.put("slot", slot);
        fields.put("sourceEntityId", sourceEntityId);
        fields.put("targetEntityId", targetEntityId);
        fields.put("targetType", targetType);
        fields.put("position", position);
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
            BattleRoom room = battleRoomManager.findRoomByEntityId((String) targetEntityId);
            if (room != null) {
                return room;
            }
        }
        Object sourceEntityId = fields.get("sourceEntityId");
        if (sourceEntityId instanceof String) {
            return battleRoomManager.findRoomByEntityId((String) sourceEntityId);
        }
        return null;
    }
}
