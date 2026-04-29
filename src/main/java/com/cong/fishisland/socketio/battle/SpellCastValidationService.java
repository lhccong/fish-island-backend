package com.cong.fishisland.socketio.battle;

import com.cong.fishisland.model.fishbattle.battle.BattleChampionState;
import com.cong.fishisland.model.fishbattle.battle.BattleMinionState;
import com.cong.fishisland.model.fishbattle.battle.BattleRoom;
import com.cong.fishisland.model.fishbattle.battle.BattleStructureState;
import com.cong.fishisland.model.fishbattle.battle.BattleVector3;
import com.cong.fishisland.model.fishbattle.battle.CastValidationResult;
import com.cong.fishisland.model.fishbattle.battle.SpellCastRequest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
    private static final boolean DEFAULT_SIMPLE_VISION_ENABLED = true;
    private static final double DEFAULT_SIGHT_RADIUS = 35D;

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
        CastValidationResult basicAttackConfigResult = checkBasicAttackConfig(caster, slot);
        if (basicAttackConfigResult != null) {
            return basicAttackConfigResult;
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
            Object target = resolveAttackableTarget(room, request.getTargetEntityId());
            if (target == null) {
                return CastValidationResult.fail("target_not_found", "目标不存在");
            }
            if (isTargetDead(target)) {
                return CastValidationResult.fail("target_dead", "目标已死亡");
            }
            CastValidationResult targetRulesResult = checkTargetRules(caster, target, cast.path("targetRules"));
            if (targetRulesResult != null) {
                return targetRulesResult;
            }
            if (target instanceof BattleChampionState
                    && !isTargetVisibleToChampion(room, caster, target)) {
                return CastValidationResult.fail("target_not_visible", "目标当前不可见");
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

    private CastValidationResult checkTargetRules(BattleChampionState caster, Object target, JsonNode targetRules) {
        if (targetRules == null || targetRules.isMissingNode()) {
            return null;
        }
        String targetId = resolveTargetId(target);
        String targetTeam = resolveTargetTeam(target);
        boolean allowSelf = targetRules.path("allowSelf").asBoolean(false);
        if (caster.getId() != null && caster.getId().equals(targetId) && !allowSelf) {
            return CastValidationResult.fail("invalid_target", "该技能不能以自己为目标");
        }
        boolean enemyOnly = targetRules.path("enemyOnly").asBoolean(false);
        if (enemyOnly && caster.getTeam() != null && caster.getTeam().equals(targetTeam)) {
            return CastValidationResult.fail("invalid_target", "该技能只能以敌方单位为目标");
        }
        boolean allyOnly = targetRules.path("allyOnly").asBoolean(false);
        if (allyOnly && (caster.getTeam() == null || !caster.getTeam().equals(targetTeam))) {
            return CastValidationResult.fail("invalid_target", "该技能只能以己方单位为目标");
        }
        return null;
    }

    private CastValidationResult checkBasicAttackConfig(BattleChampionState caster, String slot) {
        if (!"basicAttack".equals(slot)) {
            return null;
        }
        double attackRange = caster.getAttackRange() != null ? caster.getAttackRange() : 0D;
        double attackSpeed = caster.getAttackSpeed() != null ? caster.getAttackSpeed() : 0D;
        if (attackRange <= 0D) {
            return CastValidationResult.fail("invalid_attack_range", "英雄普攻距离配置无效");
        }
        if (attackSpeed <= 0D) {
            return CastValidationResult.fail("invalid_attack_speed", "英雄普攻攻速配置无效");
        }
        return null;
    }

    private CastValidationResult checkCastRange(BattleRoom room, BattleChampionState caster, SpellCastRequest request, JsonNode skillDefinition) {
        JsonNode cast = skillDefinition.path("cast");
        double maxRange = resolveCastRange(caster, request, skillDefinition);
        if (maxRange <= 0D) {
            return null;
        }
        double toleranceRange = maxRange + 0.5D;
        String castType = cast.path("type").asText("self_cast");
        if ("target_unit".equals(castType) && request.getTargetEntityId() != null) {
            Object target = resolveAttackableTarget(room, request.getTargetEntityId());
            BattleVector3 targetPosition = resolveTargetPosition(target);
            if (targetPosition != null && caster.getPosition() != null) {
                double distance = calculateDistance(caster.getPosition(), targetPosition);
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

    private double resolveCastRange(BattleChampionState caster, SpellCastRequest request, JsonNode skillDefinition) {
        String slot = skillDefinition.path("slot").asText(request != null ? request.getSlot() : null);
        if ("basicAttack".equals(slot)) {
            return caster != null && caster.getAttackRange() != null ? caster.getAttackRange() : 0D;
        }
        return skillDefinition.path("cast").path("range").asDouble(0D);
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

    private Object resolveAttackableTarget(BattleRoom room, String targetEntityId) {
        BattleChampionState champion = battleRoomManager.findChampion(room, targetEntityId).orElse(null);
        if (champion != null) {
            return champion;
        }
        BattleMinionState minion = battleRoomManager.findMinion(room, targetEntityId).orElse(null);
        if (minion != null) {
            return minion;
        }
        return battleRoomManager.findStructure(room, targetEntityId).orElse(null);
    }

    private boolean isTargetDead(Object target) {
        if (target instanceof BattleChampionState) {
            return Boolean.TRUE.equals(((BattleChampionState) target).getDead());
        }
        if (target instanceof BattleMinionState) {
            return Boolean.TRUE.equals(((BattleMinionState) target).getDead());
        }
        if (target instanceof BattleStructureState) {
            return Boolean.TRUE.equals(((BattleStructureState) target).getIsDestroyed());
        }
        return true;
    }

    private String resolveTargetId(Object target) {
        if (target instanceof BattleChampionState) {
            return ((BattleChampionState) target).getId();
        }
        if (target instanceof BattleMinionState) {
            return ((BattleMinionState) target).getId();
        }
        if (target instanceof BattleStructureState) {
            return ((BattleStructureState) target).getId();
        }
        return null;
    }

    private String resolveTargetTeam(Object target) {
        if (target instanceof BattleChampionState) {
            return ((BattleChampionState) target).getTeam();
        }
        if (target instanceof BattleMinionState) {
            return ((BattleMinionState) target).getTeam();
        }
        if (target instanceof BattleStructureState) {
            return ((BattleStructureState) target).getTeam();
        }
        return null;
    }

    private BattleVector3 resolveTargetPosition(Object target) {
        if (target instanceof BattleChampionState) {
            return ((BattleChampionState) target).getPosition();
        }
        if (target instanceof BattleMinionState) {
            return ((BattleMinionState) target).getPosition();
        }
        if (target instanceof BattleStructureState) {
            return ((BattleStructureState) target).getPosition();
        }
        return null;
    }

    private boolean isTargetVisibleToChampion(BattleRoom room, BattleChampionState viewer, Object target) {
        if (viewer == null || target == null) {
            return false;
        }
        if (!(target instanceof BattleChampionState)) {
            return true;
        }
        BattleChampionState targetChampion = (BattleChampionState) target;
        if (viewer.getId() != null && viewer.getId().equals(targetChampion.getId())) {
            return true;
        }
        if (viewer.getTeam() != null && viewer.getTeam().equals(targetChampion.getTeam())) {
            return true;
        }
        if (viewer.getPosition() == null || targetChampion.getPosition() == null) {
            return false;
        }
        if (isSimpleVisionEnabled()) {
            double sightRadius = resolveConfiguredSightRadius();
            double dx = viewer.getPosition().getX() - targetChampion.getPosition().getX();
            double dz = viewer.getPosition().getZ() - targetChampion.getPosition().getZ();
            if (dx * dx + dz * dz > sightRadius * sightRadius) {
                return false;
            }
        }
        int targetBushIndex = getBushIndex(targetChampion.getPosition().getX(), targetChampion.getPosition().getZ());
        if (targetBushIndex < 0) {
            return true;
        }
        for (BattleChampionState ally : room.getChampions()) {
            if (ally == null || ally.getPosition() == null || Boolean.TRUE.equals(ally.getDead())) {
                continue;
            }
            if (viewer.getTeam() == null || !viewer.getTeam().equals(ally.getTeam())) {
                continue;
            }
            int allyBushIndex = getBushIndex(ally.getPosition().getX(), ally.getPosition().getZ());
            if (allyBushIndex == targetBushIndex) {
                return true;
            }
        }
        return false;
    }

    private boolean isSimpleVisionEnabled() {
        JsonNode root = battleRoomManager.getLatestGameConfigRoot();
        if (root == null || root.isMissingNode()) {
            return DEFAULT_SIMPLE_VISION_ENABLED;
        }
        return root.path("vision").path("enabled").asBoolean(DEFAULT_SIMPLE_VISION_ENABLED);
    }

    private double resolveConfiguredSightRadius() {
        JsonNode root = battleRoomManager.getLatestGameConfigRoot();
        if (root == null || root.isMissingNode()) {
            return DEFAULT_SIGHT_RADIUS;
        }
        double sightRadius = root.path("vision").path("sightRadius").asDouble(DEFAULT_SIGHT_RADIUS);
        if (!Double.isFinite(sightRadius) || sightRadius < 0D) {
            return DEFAULT_SIGHT_RADIUS;
        }
        return sightRadius;
    }

    private int getBushIndex(double x, double z) {
        double[][] bushColliders = loadBushColliders();
        for (int i = 0; i < bushColliders.length; i++) {
            double[] bush = bushColliders[i];
            if (bush == null || bush.length < 4) {
                continue;
            }
            if (Math.abs(x - bush[0]) <= bush[2] && Math.abs(z - bush[1]) <= bush[3]) {
                return i;
            }
        }
        return -1;
    }

    private double[][] loadBushColliders() {
        JsonNode root = battleRoomManager.getLatestMapConfigRoot();
        if (root == null || root.isMissingNode()) {
            return new double[0][];
        }
        JsonNode colliders = root.path("bushes").path("colliders");
        if (!colliders.isArray()) {
            return new double[0][];
        }
        List<double[]> result = new ArrayList<double[]>();
        for (JsonNode node : colliders) {
            if (node == null || !node.isArray() || node.size() < 4) {
                continue;
            }
            result.add(new double[]{
                    node.get(0).asDouble(),
                    node.get(1).asDouble(),
                    node.get(2).asDouble(),
                    node.get(3).asDouble(),
            });
        }
        return result.toArray(new double[0][]);
    }

    private double calculateDistance(BattleVector3 a, BattleVector3 b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
}
