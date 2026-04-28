package com.cong.fishisland.socketio.battle;

import com.cong.fishisland.model.fishbattle.battle.ActiveSpellInstance;
import com.cong.fishisland.model.fishbattle.battle.BattleChampionState;
import com.cong.fishisland.model.fishbattle.battle.BattleRoom;
import com.cong.fishisland.model.fishbattle.battle.BattleVector3;
import com.cong.fishisland.model.fishbattle.battle.CastValidationResult;
import com.cong.fishisland.model.fishbattle.battle.SpellCastRequest;
import com.cong.fishisland.model.fishbattle.battle.SpellStageTransition;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 施法生命周期服务（main 最小版）。
 */
@Service
@RequiredArgsConstructor
public class SpellLifecycleService {
    private final SpellCastValidationService spellCastValidationService;
    private final HeroSkillDefinitionService heroSkillDefinitionService;
    private final EffectAtomicExecutor effectAtomicExecutor;
    private final Battle3dRoomManager battleRoomManager;

    private final Map<String, List<ActiveSpellInstance>> activeCasts = new ConcurrentHashMap<String, List<ActiveSpellInstance>>();

    public CastValidationResult validate(BattleRoom room, SpellCastRequest request) {
        return spellCastValidationService.validate(room, request);
    }

    public ActiveSpellInstance create(BattleRoom room, SpellCastRequest request) {
        BattleChampionState caster = battleRoomManager.findChampion(room, request.getCasterId()).orElse(null);
        if (caster == null) {
            throw new IllegalStateException("施法者不存在: " + request.getCasterId());
        }
        JsonNode skillDef = resolveSkillDefinition(caster.getHeroId(), request.getSkillId(), request.getSlot());
        if (skillDef == null || skillDef.isMissingNode()) {
            throw new IllegalStateException("技能定义不存在: " + request.getSkillId());
        }

        long now = System.currentTimeMillis();
        JsonNode castDef = skillDef.path("cast");
        long castTimeMs = resolveCastTimeMs(castDef);
        long backswingMs = resolveBackswingMs(castDef);
        boolean lockMovement = resolveLockMovement(castDef);

        ActiveSpellInstance instance = ActiveSpellInstance.builder()
                .castInstanceId("cast-" + UUID.randomUUID())
                .requestId(request.getRequestId())
                .roomId(room.getRoomId())
                .casterId(request.getCasterId())
                .skillId(skillDef.path("skillId").asText(request.getSkillId()))
                .slot(skillDef.path("slot").asText(request.getSlot()))
                .stage("windup")
                .targetEntityId(request.getTargetEntityId())
                .targetPoint(request.getTargetPoint())
                .targetDirection(request.getTargetDirection())
                .createdAt(now)
                .stageStartedAt(now)
                .expectedResolveAt(now + castTimeMs)
                .build();

        getRoomCasts(room.getRoomId()).add(instance);
        caster.setActiveCastInstanceId(instance.getCastInstanceId());
        caster.setActiveCastPhase(castTimeMs > 0 ? "windup" : "resolve");

        applySkillCostAndCooldown(caster, instance.getSlot(), skillDef);
        if (lockMovement) {
            caster.setMovementLockedUntil(Math.max(caster.getMovementLockedUntil() != null ? caster.getMovementLockedUntil() : 0L, now + castTimeMs + backswingMs));
            caster.setMoveTarget(null);
            caster.setInputMode("idle");
        }

        if (castTimeMs <= 0L) {
            resolveSpell(room, instance);
        }
        return instance;
    }

    public List<SpellStageTransition> tickSpellStages(BattleRoom room, long now) {
        List<SpellStageTransition> transitions = new ArrayList<SpellStageTransition>();
        if (room == null) {
            return transitions;
        }
        List<ActiveSpellInstance> instances = getRoomCasts(room.getRoomId());
        if (instances.isEmpty()) {
            return transitions;
        }
        for (ActiveSpellInstance instance : new ArrayList<ActiveSpellInstance>(instances)) {
            if ("windup".equals(instance.getStage()) && now >= safeTime(instance.getExpectedResolveAt())) {
                String previousStage = instance.getStage();
                resolveSpell(room, instance);
                transitions.add(SpellStageTransition.builder()
                        .castInstanceId(instance.getCastInstanceId())
                        .casterId(instance.getCasterId())
                        .skillId(instance.getSkillId())
                        .slot(instance.getSlot())
                        .targetEntityId(instance.getTargetEntityId())
                        .targetPoint(instance.getTargetPoint())
                        .previousStage(previousStage)
                        .nextStage(instance.getStage())
                        .build());
            } else if ("backswing".equals(instance.getStage()) && now >= safeTime(instance.getExpectedResolveAt())) {
                finishSpell(room, instance);
                transitions.add(SpellStageTransition.builder()
                        .castInstanceId(instance.getCastInstanceId())
                        .casterId(instance.getCasterId())
                        .skillId(instance.getSkillId())
                        .slot(instance.getSlot())
                        .targetEntityId(instance.getTargetEntityId())
                        .targetPoint(instance.getTargetPoint())
                        .previousStage("backswing")
                        .nextStage("finished")
                        .build());
            }
        }
        instances.removeIf(instance -> "finished".equals(instance.getStage()));
        return transitions;
    }

    private void resolveSpell(BattleRoom room, ActiveSpellInstance instance) {
        BattleChampionState caster = battleRoomManager.findChampion(room, instance.getCasterId()).orElse(null);
        if (caster == null) {
            instance.setStage("finished");
            return;
        }
        JsonNode skillDef = resolveSkillDefinition(caster.getHeroId(), instance.getSkillId(), instance.getSlot());
        if (skillDef == null || skillDef.isMissingNode()) {
            finishSpell(room, instance);
            return;
        }

        executeEffectList(room, instance, caster, skillDef.path("effects").path("onActivate"), skillDef.path("cast"));
        executeEffectList(room, instance, caster, skillDef.path("effects").path("onImpact"), skillDef.path("cast"));
        executeEffectList(room, instance, caster, skillDef.path("effects").path("onSuccessCast"), skillDef.path("cast"));

        long backswingMs = resolveBackswingMs(skillDef.path("cast"));
        if (backswingMs > 0L) {
            instance.setStage("backswing");
            instance.setStageStartedAt(System.currentTimeMillis());
            instance.setExpectedResolveAt(System.currentTimeMillis() + backswingMs);
            caster.setActiveCastPhase("backswing");
        } else {
            finishSpell(room, instance);
        }
    }

    private void finishSpell(BattleRoom room, ActiveSpellInstance instance) {
        instance.setStage("finished");
        BattleChampionState caster = battleRoomManager.findChampion(room, instance.getCasterId()).orElse(null);
        if (caster != null) {
            caster.setActiveCastInstanceId(null);
            caster.setActiveCastPhase("idle");
            Map<String, Map<String, Object>> skillStates = ensureSkillStates(caster);
            Map<String, Object> slotState = ensureSlotState(skillStates, instance.getSlot(), instance.getSkillId());
            slotState.put("isCasting", Boolean.FALSE);
            long remaining = readLongValue(slotState.get("remainingCooldownMs"), 0L);
            slotState.put("isReady", remaining <= 0L);
        }
    }

    private void executeEffectList(BattleRoom room, ActiveSpellInstance instance, BattleChampionState caster,
                                   JsonNode effects, JsonNode castDef) {
        if (effects == null || !effects.isArray()) {
            return;
        }
        for (JsonNode effect : effects) {
            executeEffect(room, instance, caster, effect, castDef);
        }
    }

    private void executeEffect(BattleRoom room, ActiveSpellInstance instance, BattleChampionState caster,
                               JsonNode effect, JsonNode castDef) {
        String type = normalizeEffectType(effect.path("type").asText(""));
        switch (type) {
            case "heal":
                executeHealEffect(room, instance, caster, effect, castDef);
                break;
            case "applystatus":
                executeApplyStatusEffect(room, instance, caster, effect, castDef);
                break;
            case "teleport":
                executeTeleportEffect(caster, instance);
                break;
            case "damage":
                executeDamageEffect(room, instance, caster, effect, castDef);
                break;
            default:
                break;
        }
    }

    private void executeHealEffect(BattleRoom room, ActiveSpellInstance instance, BattleChampionState caster,
                                   JsonNode effect, JsonNode castDef) {
        double amount = effect.path("amount").asDouble(effect.path("base").asDouble(0D));
        String targetMode = resolveTargetMode(effect, castDef, "self");
        List<BattleChampionState> targets = resolveTargets(room, instance, caster, effect, targetMode);
        for (BattleChampionState target : targets) {
            effectAtomicExecutor.applyHeal(caster.getId(), instance.getCastInstanceId(), instance.getSkillId(), instance.getSlot(), target, amount);
        }
    }

    private void executeApplyStatusEffect(BattleRoom room, ActiveSpellInstance instance, BattleChampionState caster,
                                          JsonNode effect, JsonNode castDef) {
        String statusId = effect.path("statusId").asText("");
        long durationMs = effect.path("durationMs").asLong(0L);
        int stacks = effect.path("stacks").asInt(1);
        String targetMode = resolveTargetMode(effect, castDef, "self");
        List<BattleChampionState> targets = resolveTargets(room, instance, caster, effect, targetMode);
        for (BattleChampionState target : targets) {
            effectAtomicExecutor.applyStatus(instance.getCastInstanceId(), instance.getSkillId(), instance.getSlot(), statusId, caster.getId(), target, stacks, durationMs);
        }
    }

    private void executeDamageEffect(BattleRoom room, ActiveSpellInstance instance, BattleChampionState caster,
                                     JsonNode effect, JsonNode castDef) {
        double amount = effect.path("amount").asDouble(effect.path("base").asDouble(0D));
        String damageType = effect.path("damageType").asText("physical");
        String targetMode = resolveTargetMode(effect, castDef, "single");
        List<BattleChampionState> targets = resolveTargets(room, instance, caster, effect, targetMode);
        for (BattleChampionState target : targets) {
            effectAtomicExecutor.applyDamage(caster.getId(), instance.getCastInstanceId(), instance.getSkillId(), instance.getSlot(), target, amount, damageType);
        }
    }

    private void executeTeleportEffect(BattleChampionState caster, ActiveSpellInstance instance) {
        if (caster == null) {
            return;
        }
        BattleVector3 destination = instance.getTargetPoint() != null
                ? BattleVector3.builder()
                    .x(clamp(instance.getTargetPoint().getX(), battleRoomManager.getMapXMin(), battleRoomManager.getMapXMax()))
                    .y(instance.getTargetPoint().getY())
                    .z(clamp(instance.getTargetPoint().getZ(), battleRoomManager.getMapZMin(), battleRoomManager.getMapZMax()))
                    .build()
                : null;
        effectAtomicExecutor.applyTeleport(caster, destination);
    }

    private List<BattleChampionState> resolveTargets(BattleRoom room, ActiveSpellInstance instance, BattleChampionState caster,
                                                     JsonNode effect, String targetMode) {
        List<BattleChampionState> targets = new ArrayList<BattleChampionState>();
        if (room == null || caster == null) {
            return targets;
        }
        if ("self".equals(targetMode)) {
            targets.add(caster);
            return filterTargets(caster, targets, effect.path("targetRules"));
        }
        if ("single".equals(targetMode) || "target_unit".equals(targetMode)) {
            if (instance.getTargetEntityId() == null) {
                return targets;
            }
            BattleChampionState target = battleRoomManager.findChampion(room, instance.getTargetEntityId()).orElse(null);
            if (target != null && !Boolean.TRUE.equals(target.getDead())) {
                targets.add(target);
            }
            return filterTargets(caster, targets, effect.path("targetRules"));
        }
        if ("radius".equals(targetMode)) {
            double radius = effect.path("radius").asDouble(0D);
            BattleVector3 center = instance.getTargetPoint() != null ? instance.getTargetPoint() : caster.getPosition();
            for (BattleChampionState champion : room.getChampions()) {
                if (champion == null || champion.getPosition() == null || Boolean.TRUE.equals(champion.getDead())) {
                    continue;
                }
                if (distance(center, champion.getPosition()) <= radius + 0.5D) {
                    targets.add(champion);
                }
            }
            return filterTargets(caster, targets, effect.path("targetRules"));
        }
        return targets;
    }

    private List<BattleChampionState> filterTargets(BattleChampionState caster, List<BattleChampionState> targets, JsonNode targetRules) {
        if (targetRules == null || targetRules.isMissingNode()) {
            return targets;
        }
        boolean enemyOnly = targetRules.path("enemyOnly").asBoolean(false);
        boolean allyOnly = targetRules.path("allyOnly").asBoolean(false);
        boolean allowSelf = targetRules.path("allowSelf").asBoolean(true);
        List<BattleChampionState> filtered = new ArrayList<BattleChampionState>();
        for (BattleChampionState target : targets) {
            if (target == null) {
                continue;
            }
            if (!allowSelf && caster.getId() != null && caster.getId().equals(target.getId())) {
                continue;
            }
            if (enemyOnly && isSameTeam(caster, target)) {
                continue;
            }
            if (allyOnly && !isSameTeam(caster, target)) {
                continue;
            }
            filtered.add(target);
        }
        return filtered;
    }

    private void applySkillCostAndCooldown(BattleChampionState caster, String slot, JsonNode skillDef) {
        Map<String, Map<String, Object>> skillStates = ensureSkillStates(caster);
        Map<String, Object> slotState = ensureSlotState(skillStates, slot, skillDef.path("skillId").asText(slot));
        long cooldownMs = skillDef.path("cooldown").path("baseMs").asLong(0L);
        slotState.put("slot", slot);
        slotState.put("skillId", skillDef.path("skillId").asText(slot));
        slotState.put("name", skillDef.path("name").asText(slot));
        slotState.put("level", skillDef.path("initialLevel").asInt(1));
        slotState.put("maxCooldownMs", cooldownMs);
        slotState.put("remainingCooldownMs", cooldownMs);
        slotState.put("isReady", cooldownMs <= 0L);
        slotState.put("isCasting", Boolean.TRUE);
        slotState.put("insufficientResource", Boolean.FALSE);
        slotState.put("isSecondPhase", Boolean.FALSE);

        JsonNode cost = skillDef.path("cost");
        if ("mana".equals(cost.path("resourceType").asText("none")) && caster.getMp() != null) {
            double amount = cost.path("amount").asDouble(0D);
            caster.setMp(Math.max(0D, caster.getMp() - amount));
        }
    }

    private Map<String, Map<String, Object>> ensureSkillStates(BattleChampionState caster) {
        if (caster.getSkillStates() == null) {
            caster.setSkillStates(new LinkedHashMap<String, Map<String, Object>>());
        }
        return caster.getSkillStates();
    }

    private Map<String, Object> ensureSlotState(Map<String, Map<String, Object>> skillStates, String slot, String skillId) {
        Map<String, Object> slotState = skillStates.get(slot);
        if (slotState == null) {
            slotState = new LinkedHashMap<String, Object>();
            slotState.put("slot", slot);
            slotState.put("skillId", skillId);
            slotState.put("level", 1);
            slotState.put("maxCooldownMs", 0L);
            slotState.put("remainingCooldownMs", 0L);
            slotState.put("isReady", Boolean.TRUE);
            slotState.put("insufficientResource", Boolean.FALSE);
            slotState.put("isSecondPhase", Boolean.FALSE);
            slotState.put("isCasting", Boolean.FALSE);
            skillStates.put(slot, slotState);
        }
        return slotState;
    }

    private JsonNode resolveSkillDefinition(String heroId, String skillId, String slot) {
        JsonNode skillDef = heroSkillDefinitionService.findSkillById(heroId, skillId, slot);
        if (skillDef != null && !skillDef.isMissingNode()) {
            return skillDef;
        }
        return heroSkillDefinitionService.findSkillBySlot(heroId, slot);
    }

    private List<ActiveSpellInstance> getRoomCasts(String roomId) {
        return activeCasts.computeIfAbsent(roomId, key -> new CopyOnWriteArrayList<ActiveSpellInstance>());
    }

    private String normalizeEffectType(String effectType) {
        if (effectType == null) {
            return "";
        }
        if ("ApplyBuff".equals(effectType) || "ApplyStatus".equals(effectType)) {
            return "applystatus";
        }
        return effectType.trim().toLowerCase();
    }

    private String resolveTargetMode(JsonNode effect, JsonNode castDef, String fallback) {
        if (effect.has("targetMode")) {
            return effect.path("targetMode").asText(fallback);
        }
        if (castDef != null && castDef.has("type")) {
            return castDef.path("type").asText(fallback);
        }
        return fallback;
    }

    private long resolveCastTimeMs(JsonNode cast) {
        if (cast == null || cast.isMissingNode()) {
            return 0L;
        }
        return Math.max(0L, cast.path("castTimeMs").asLong(cast.path("windupMs").asLong(0L)));
    }

    private long resolveBackswingMs(JsonNode cast) {
        if (cast == null || cast.isMissingNode()) {
            return 0L;
        }
        return Math.max(0L, cast.path("backswingMs").asLong(0L));
    }

    private boolean resolveLockMovement(JsonNode cast) {
        if (cast == null || cast.isMissingNode()) {
            return false;
        }
        if (cast.has("lockMovement")) {
            return cast.path("lockMovement").asBoolean(false);
        }
        if (cast.has("allowMoveDuringCast")) {
            return !cast.path("allowMoveDuringCast").asBoolean(true);
        }
        return false;
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

    private double distance(BattleVector3 a, BattleVector3 b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private long safeTime(Long value) {
        return value != null ? value : 0L;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
