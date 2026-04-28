package com.cong.fishisland.socketio.battle;

import com.cong.fishisland.model.fishbattle.battle.BattleChampionState;
import com.cong.fishisland.model.fishbattle.battle.BattleRoom;
import com.cong.fishisland.model.fishbattle.battle.BattleVector3;
import com.cong.fishisland.model.fishbattle.battle.CastValidationResult;
import com.cong.fishisland.model.fishbattle.battle.SpellCastRequest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 统一施法校验服务（main 最小版）。
 */
@Service
@RequiredArgsConstructor
public class SpellCastValidationService {
    private final Battle3dRoomManager battleRoomManager;
    private final HeroSkillDefinitionService heroSkillDefinitionService;
    private final StatusEffectService statusEffectService;

    public CastValidationResult validate(BattleRoom room, SpellCastRequest request) {
        if (request == null) {
            return CastValidationResult.fail("invalid_request", "施法请求为空");
        }
        if (room == null) {
            return CastValidationResult.fail("room_not_found", "战斗房间不存在");
        }
        if (request.getCasterId() == null || request.getCasterId().trim().isEmpty()) {
            return CastValidationResult.fail("invalid_caster", "施法者不能为空");
        }

        BattleChampionState caster = battleRoomManager.findChampion(room, request.getCasterId()).orElse(null);
        if (caster == null) {
            return CastValidationResult.fail("caster_not_found", "施法者不存在");
        }
        if (Boolean.TRUE.equals(caster.getDead())) {
            return CastValidationResult.fail("caster_dead", "施法者已死亡");
        }
        if (isCrowdControlled(room, caster)) {
            return CastValidationResult.fail("crowd_controlled", "当前处于受控状态，无法施法");
        }
        if (caster.getActiveCastPhase() != null
                && !"idle".equals(caster.getActiveCastPhase())
                && !"finished".equals(caster.getActiveCastPhase())) {
            return CastValidationResult.fail("already_casting", "当前正在施法中");
        }

        JsonNode skillDefinition = resolveSkillDefinition(caster.getHeroId(), request.getSkillId(), request.getSlot());
        if (skillDefinition == null || skillDefinition.isMissingNode()) {
            return CastValidationResult.fail("skill_not_found", "技能定义不存在");
        }

        String slot = skillDefinition.path("slot").asText(request.getSlot());
        CastValidationResult cooldownResult = checkCooldown(caster, slot);
        if (cooldownResult != null) {
            return cooldownResult;
        }

        JsonNode cast = skillDefinition.path("cast");
        String castType = cast.path("type").asText("self_cast");
        if ("target_unit".equals(castType) && (request.getTargetEntityId() == null || request.getTargetEntityId().trim().isEmpty())) {
            return CastValidationResult.fail("invalid_target", "该技能需要目标单位");
        }
        if (("target_point".equals(castType) || "directional".equals(castType)) && request.getTargetPoint() == null) {
            return CastValidationResult.fail("invalid_point", "该技能需要目标点");
        }

        if ("target_unit".equals(castType) && request.getTargetEntityId() != null) {
            BattleChampionState target = battleRoomManager.findChampion(room, request.getTargetEntityId()).orElse(null);
            if (target == null) {
                return CastValidationResult.fail("target_not_found", "目标不存在");
            }
            if (Boolean.TRUE.equals(target.getDead())) {
                return CastValidationResult.fail("target_dead", "目标已死亡");
            }
            CastValidationResult targetRulesResult = checkTargetRules(caster, target, cast.path("targetRules"));
            if (targetRulesResult != null) {
                return targetRulesResult;
            }
        }

        CastValidationResult rangeResult = checkCastRange(room, caster, request, skillDefinition);
        if (rangeResult != null) {
            return rangeResult;
        }
        return CastValidationResult.success();
    }

    private JsonNode resolveSkillDefinition(String heroId, String skillId, String slot) {
        if (skillId != null && !skillId.trim().isEmpty()) {
            JsonNode result = heroSkillDefinitionService.findSkillById(heroId, skillId, slot);
            if (result != null && !result.isMissingNode()) {
                return result;
            }
        }
        return heroSkillDefinitionService.findSkillBySlot(heroId, slot);
    }

    private boolean isCrowdControlled(BattleRoom room, BattleChampionState caster) {
        String roomId = room.getRoomId();
        String casterId = caster.getId();
        return statusEffectService.hasStatus(roomId, casterId, "stun")
                || statusEffectService.hasStatus(roomId, casterId, "airborne")
                || statusEffectService.hasStatus(roomId, casterId, "silence")
                || statusEffectService.hasStatus(roomId, casterId, "suppression");
    }

    private CastValidationResult checkCooldown(BattleChampionState caster, String slot) {
        Map<String, Map<String, Object>> skillStates = caster.getSkillStates();
        if (skillStates == null || slot == null) {
            return null;
        }
        Map<String, Object> slotState = skillStates.get(slot);
        if (slotState == null) {
            return null;
        }
        Object isCastingObj = slotState.get("isCasting");
        if (Boolean.TRUE.equals(isCastingObj)) {
            return CastValidationResult.fail("already_casting_slot", "该技能正在施法中");
        }
        long remainingCooldownMs = readLongValue(slotState.get("remainingCooldownMs"), 0L);
        if (remainingCooldownMs > 0L) {
            return CastValidationResult.fail("on_cooldown", "技能冷却中");
        }
        return null;
    }

    private CastValidationResult checkTargetRules(BattleChampionState caster, BattleChampionState target, JsonNode targetRules) {
        if (targetRules == null || targetRules.isMissingNode()) {
            return null;
        }
        boolean allowSelf = targetRules.path("allowSelf").asBoolean(false);
        if (caster.getId() != null && caster.getId().equals(target.getId()) && !allowSelf) {
            return CastValidationResult.fail("invalid_target", "该技能不能以自己为目标");
        }
        boolean enemyOnly = targetRules.path("enemyOnly").asBoolean(false);
        if (enemyOnly && isSameTeam(caster, target)) {
            return CastValidationResult.fail("invalid_target", "该技能只能以敌方单位为目标");
        }
        boolean allyOnly = targetRules.path("allyOnly").asBoolean(false);
        if (allyOnly && !isSameTeam(caster, target)) {
            return CastValidationResult.fail("invalid_target", "该技能只能以己方单位为目标");
        }
        return null;
    }

    private CastValidationResult checkCastRange(BattleRoom room, BattleChampionState caster, SpellCastRequest request, JsonNode skillDefinition) {
        JsonNode cast = skillDefinition.path("cast");
        double maxRange = cast.path("range").asDouble(0D);
        if (maxRange <= 0D) {
            return null;
        }
        double toleranceRange = maxRange + 0.5D;
        String castType = cast.path("type").asText("self_cast");
        if ("target_unit".equals(castType) && request.getTargetEntityId() != null) {
            BattleChampionState target = battleRoomManager.findChampion(room, request.getTargetEntityId()).orElse(null);
            if (target != null && target.getPosition() != null && caster.getPosition() != null) {
                double distance = calculateDistance(caster.getPosition(), target.getPosition());
                if (distance > toleranceRange) {
                    return CastValidationResult.fail("out_of_range", "目标超出施法距离");
                }
            }
        }
        if (("target_point".equals(castType) || "directional".equals(castType)) && request.getTargetPoint() != null && caster.getPosition() != null) {
            double distance = calculateDistance(caster.getPosition(), request.getTargetPoint());
            if (distance > toleranceRange) {
                return CastValidationResult.fail("out_of_range", "目标点超出施法距离");
            }
        }
        return null;
    }

    private long readLongValue(Object value, long fallback) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private boolean isSameTeam(BattleChampionState a, BattleChampionState b) {
        return a != null && b != null && a.getTeam() != null && a.getTeam().equals(b.getTeam());
    }

    private double calculateDistance(BattleVector3 a, BattleVector3 b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
}
