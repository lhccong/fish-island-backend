package com.cong.fishisland.socketio.battle;

import com.cong.fishisland.model.fishbattle.battle.*;
import com.cong.fishisland.config.fishbattle.FishBattleServerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 战斗模拟引擎。
 * 以固定间隔推进游戏状态（Tick），并定期广播战斗快照。
 * 参考原项目完整实现：小兵 AI、防御塔 AI、延迟伤害管线、建筑保护链等。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BattleSimulationService {
    private final Battle3dRoomManager battleRoomManager;
    private final Battle3dBroadcastService battleBroadcastService;
    private final InputQueue inputQueue;
    private final SpellCastHandler spellCastHandler;
    private final SpellLifecycleService spellLifecycleService;
    private final StatusEffectService statusEffectService;
    private final EffectAtomicExecutor effectAtomicExecutor;
    private final FishBattleServerProperties fishBattleServerProperties;

    private static final double MINION_REACHED_TARGET_EPSILON = 0.15D;
    private static final double CHAMPION_COLLISION_RADIUS = 0.5D;
    private static final double TOWER_PROJECTILE_SPEED = 18D;
    private static final long MINION_MELEE_WINDUP_MS = 180L;
    private static final double MINION_CASTER_PROJECTILE_SPEED = 12D;
    private static final double MINION_Z_CONVERGE_FACTOR = 0.35D;
    private static final long MINION_DEATH_CLEANUP_MS = 1500L;
    private static final int MAX_CATCHUP_TICKS = 4;

    private static final String[][] STRUCTURE_ATTACK_ORDER = {
            {"tower", "outer"},
            {"tower", "inner"},
            {"inhibitor", null},
            {"tower", "nexusGuard"},
            {"nexus", null},
    };

    /* ── 以下字段在 start() 中初始化 ── */
    private double mapXMin;
    private double mapXMax;
    private double mapZMin;
    private double mapZMax;
    private long minionSpawnIntervalMs;
    /** 建筑碰撞体列表：{x, z, radius}。 */
    private double[][] structureColliders;
    /** 碰撞体队伍标识，与 structureColliders 一一对应。 */
    private String[] structureColliderTeams;
    /** 草丛碰撞体缓存：每个元素 [centerX, centerZ, halfW, halfD]。 */
    private double[][] bushColliders;

    private static final class TickComputationContext {
        private final Map<String, Set<String>> teamBushIds = new HashMap<String, Set<String>>();
        private final Map<String, String> championBushIds = new HashMap<String, String>();
    }

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> tickFuture;

    /** 累积器：未消耗的时间（纳秒） */
    private long accumulatorNs;
    /** 上一次 loop 时间戳（纳秒） */
    private long previousLoopNs;
    /** 固定 tick 步长（纳秒） */
    private long fixedTickStepNs;
    /** 每隔多少个 tick 广播一次快照 */
    private int snapshotEveryNTicks;

    @PostConstruct
    public void start() {
        mapXMin = battleRoomManager.getMapXMin();
        mapXMax = battleRoomManager.getMapXMax();
        mapZMin = battleRoomManager.getMapZMin();
        mapZMax = battleRoomManager.getMapZMax();

        initStructureColliders();
        initMinionConfig();
        bushColliders = loadBushColliders();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "battle-tick");
            t.setDaemon(true);
            return t;
        });
        long tickIntervalMs = 50L;
        long snapshotIntervalMs = 50L;
        fixedTickStepNs = tickIntervalMs * 1_000_000L;
        snapshotEveryNTicks = Math.max(1, (int) (snapshotIntervalMs / tickIntervalMs));
        accumulatorNs = 0;
        previousLoopNs = System.nanoTime();
        long loopIntervalMs = Math.max(1, tickIntervalMs / 2);
        tickFuture = scheduler.scheduleAtFixedRate(this::loop, loopIntervalMs, loopIntervalMs, TimeUnit.MILLISECONDS);
        log.info("战斗模拟引擎启动，Tick间隔={}ms，快照间隔={}tick({}ms)",
                tickIntervalMs, snapshotEveryNTicks, snapshotEveryNTicks * tickIntervalMs);
    }

    @PreDestroy
    public void stop() {
        if (tickFuture != null) {
            tickFuture.cancel(false);
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
        log.info("战斗模拟引擎已停止");
    }

    public void destroyRoom(String roomCode) {
        if (roomCode == null) {
            return;
        }
        inputQueue.cleanupRoom(roomCode);
        battleRoomManager.destroyRoom(roomCode);
        log.info("战斗房间及关联数据已销毁: roomCode={}", roomCode);
    }

    // ==================== Tick 循环 ====================

    private void loop() {
        try {
            long currentNs = System.nanoTime();
            long elapsedNs = currentNs - previousLoopNs;
            previousLoopNs = currentNs;
            if (elapsedNs < 0) {
                elapsedNs = fixedTickStepNs;
            }
            accumulatorNs += Math.min(elapsedNs, fixedTickStepNs * MAX_CATCHUP_TICKS);

            int ticksThisLoop = 0;
            while (accumulatorNs >= fixedTickStepNs && ticksThisLoop < MAX_CATCHUP_TICKS) {
                accumulatorNs -= fixedTickStepNs;
                ticksThisLoop++;
                doFixedTick();
            }
        } catch (Exception e) {
            log.error("Loop 执行异常", e);
        }
    }

    private void doFixedTick() {
        try {
            long now = System.currentTimeMillis();
            double deltaSeconds = fixedTickStepNs / 1_000_000_000.0;
            Collection<BattleRoom> rooms = battleRoomManager.getActiveRooms();
            if (rooms == null || rooms.isEmpty()) {
                return;
            }
            for (BattleRoom room : rooms) {
                tickRoom(room, now, deltaSeconds);
            }
        } catch (Exception e) {
            log.error("Tick 执行异常", e);
        }
    }

    private void tickRoom(BattleRoom room, long now, double deltaSeconds) {
        if (room == null) {
            return;
        }
        long currentTick = (room.getTickNumber() != null ? room.getTickNumber() : 0L) + 1;
        room.setTickNumber(currentTick);
        TickComputationContext context = new TickComputationContext();

        drainAndApplyInputs(room, now);
        tickStatusEffects(room, now);
        tickSpellStages(room, now);
        tickCooldowns(room, deltaSeconds);
        tickMinions(room, now, deltaSeconds, context);
        tickTowers(room, now, context);
        resolvePendingAttacks(room, now);
        tickMovement(room, deltaSeconds, now);
        tickHealthRelics(room, now);
        room.setGameTimer(room.getGameTimer() + deltaSeconds);

        if (snapshotEveryNTicks <= 1 || currentTick % snapshotEveryNTicks == 0) {
            emitSnapshot(room, now, context);
        }
    }

    // ==================== 输入处理 ====================

    private void drainAndApplyInputs(BattleRoom room, long now) {
        List<PlayerInput> inputs = inputQueue.drainByRoom(room.getRoomId());
        for (PlayerInput input : inputs) {
            applySingleInput(room, input, now);
        }
    }

    private void applySingleInput(BattleRoom room, PlayerInput input, long now) {
        if (input == null || input.getChampionId() == null) {
            return;
        }
        String championId = input.getChampionId();
        Optional<BattleChampionState> championOpt = room.getChampions().stream()
                .filter(c -> championId.equals(c.getId()))
                .findFirst();
        if (!championOpt.isPresent()) {
            return;
        }
        BattleChampionState champion = championOpt.get();

        if (Boolean.TRUE.equals(champion.getDead())) {
            return;
        }

        switch (input.getType()) {
            case MOVE:
                applyMoveInput(champion, input, now);
                break;
            case STOP:
                applyStopInput(champion, input, now);
                break;
            case CAST_SPELL:
            case BASIC_ATTACK:
                applyCastInput(room, champion, input, now);
                break;
            default:
                break;
        }
    }

    private void applyMoveInput(BattleChampionState champion, PlayerInput input, long now) {
        if (input.getTargetX() == null || input.getTargetZ() == null) {
            return;
        }
        if (champion.getMovementLockedUntil() != null && champion.getMovementLockedUntil() > now) {
            champion.setMoveTarget(null);
            champion.setInputMode("idle");
            acknowledgeMovementInput(champion, input, now);
            return;
        }
        double rawX = input.getTargetX();
        double rawZ = input.getTargetZ();
        if (!Double.isFinite(rawX)) { rawX = 0; }
        if (!Double.isFinite(rawZ)) { rawZ = 0; }
        double clampedX = clamp(rawX, mapXMin, mapXMax);
        double clampedZ = clamp(rawZ, mapZMin, mapZMax);
        champion.setMoveTarget(BattleVector3.builder().x(clampedX).y(0D).z(clampedZ).build());
        champion.setInputMode(input.getInputMode() != null ? input.getInputMode() : "mouse");
        acknowledgeMovementInput(champion, input, now);
    }

    private void acknowledgeMovementInput(BattleChampionState champion, PlayerInput input, long now) {
        if (input.getClientSeq() != null) {
            champion.setLastProcessedInputSeq(input.getClientSeq());
            champion.setLastProcessedMoveSequence(input.getClientSeq());
        }
        if (input.getClientTimestamp() != null) {
            champion.setLastMoveCommandClientTime(input.getClientTimestamp());
        }
        champion.setLastMoveCommandServerTime(now);
    }

    private void applyStopInput(BattleChampionState champion, PlayerInput input, long now) {
        champion.setMoveTarget(null);
        champion.setInputMode("idle");
        champion.setAnimationState("idle");
        champion.setIdleStartedAt(now);
        if (input.getClientSeq() != null) {
            champion.setLastProcessedInputSeq(input.getClientSeq());
            champion.setLastProcessedMoveSequence(input.getClientSeq());
        }
        if (input.getClientTimestamp() != null) {
            champion.setLastMoveCommandClientTime(input.getClientTimestamp());
        }
        champion.setLastMoveCommandServerTime(now);
        correctPositionFromClient(champion, input);
    }

    private void applyCastInput(BattleRoom room, BattleChampionState champion, PlayerInput input, long now) {
        if (input.getRawPayload() == null) {
            return;
        }
        champion.setLastMoveCommandServerTime(now);
        spellCastHandler.handleCastFromQueue(room, champion, input);
    }

    private void correctPositionFromClient(BattleChampionState champion, PlayerInput input) {
        if (input.getClientPositionX() == null || input.getClientPositionZ() == null) {
            return;
        }
        BattleVector3 pos = champion.getPosition();
        if (pos == null) {
            return;
        }
        double cx = input.getClientPositionX();
        double cz = input.getClientPositionZ();
        if (!Double.isFinite(cx) || !Double.isFinite(cz)) {
            return;
        }
        cx = clamp(cx, mapXMin, mapXMax);
        cz = clamp(cz, mapZMin, mapZMax);
        double speed = champion.getMoveSpeed() != null ? champion.getMoveSpeed() : 3.0;
        double maxDist = speed * 0.5;
        double dx = cx - pos.getX();
        double dz = cz - pos.getZ();
        if (dx * dx + dz * dz <= maxDist * maxDist) {
            pos.setX(cx);
            pos.setZ(cz);
        }
    }

    // ==================== 移动 Tick ====================

    private void tickMovement(BattleRoom room, double deltaSeconds, long now) {
        for (BattleChampionState champion : room.getChampions()) {
            if (champion.getDead() != null && champion.getDead()) {
                continue;
            }
            double effectiveMoveSpeed = resolveEffectiveMoveSpeed(room, champion);
            champion.setMoveSpeed(effectiveMoveSpeed);
            if (champion.getMovementLockedUntil() != null && champion.getMovementLockedUntil() > now) {
                champion.setMoveTarget(null);
                champion.setInputMode("idle");
                continue;
            }
            BattleVector3 target = champion.getMoveTarget();
            if (target == null) {
                if ("run".equals(champion.getAnimationState())) {
                    champion.setAnimationState("idle");
                    champion.setIdleStartedAt(now);
                }
                continue;
            }
            BattleVector3 pos = champion.getPosition();
            if (pos == null) {
                continue;
            }
            double dx = target.getX() - pos.getX();
            double dz = target.getZ() - pos.getZ();
            double dist = Math.hypot(dx, dz);
            if (dist <= 0.08) {
                pos.setX(target.getX());
                pos.setZ(target.getZ());
                champion.setMoveTarget(null);
                champion.setInputMode("idle");
                champion.setAnimationState("idle");
                champion.setIdleStartedAt(now);
                continue;
            }
            double step = Math.min(dist, effectiveMoveSpeed * deltaSeconds);
            double dirX = dx / dist;
            double dirZ = dz / dist;
            pos.setX(clamp(pos.getX() + dirX * step, mapXMin, mapXMax));
            pos.setZ(clamp(pos.getZ() + dirZ * step, mapZMin, mapZMax));
            double desiredRotation = Math.atan2(dirX, dirZ);
            champion.setRotation(desiredRotation);
            champion.setAnimationState("run");
            champion.setInputMode("mouse");
        }
    }

    private void tickStatusEffects(BattleRoom room, long now) {
        statusEffectService.cleanupExpired(room.getRoomId(), now);
    }

    private void tickSpellStages(BattleRoom room, long now) {
        List<SpellStageTransition> transitions = spellLifecycleService.tickSpellStages(room, now);
        broadcastSpellTransitions(room, transitions);
    }

    private void broadcastSpellTransitions(BattleRoom room, List<SpellStageTransition> transitions) {
        if (transitions == null || transitions.isEmpty()) {
            return;
        }
        for (SpellStageTransition transition : transitions) {
            Map<String, Object> payload = battleBroadcastService.createBaseCombatEvent(room, "spell-stage", System.currentTimeMillis());
            payload.put("castInstanceId", transition.getCastInstanceId());
            payload.put("casterId", transition.getCasterId());
            payload.put("skillId", transition.getSkillId());
            payload.put("slot", transition.getSlot());
            payload.put("previousStage", transition.getPreviousStage());
            payload.put("nextStage", transition.getNextStage());
            if (transition.getTargetEntityId() != null) {
                payload.put("targetEntityId", transition.getTargetEntityId());
            }
            if (transition.getTargetPoint() != null) {
                payload.put("targetPoint", transition.getTargetPoint());
            }
            battleBroadcastService.broadcast(room, "spellStageTransition", payload);
        }
    }

    private void tickCooldowns(BattleRoom room, double deltaSeconds) {
        long deltaMs = Math.max(1L, Math.round(deltaSeconds * 1000D));
        for (BattleChampionState champion : room.getChampions()) {
            if (champion.getSkillStates() == null) {
                continue;
            }
            for (Map<String, Object> slotState : champion.getSkillStates().values()) {
                if (slotState == null) {
                    continue;
                }
                long remainingCooldownMs = readLongValue(slotState.get("remainingCooldownMs"), 0L);
                if (remainingCooldownMs <= 0L) {
                    if (!Boolean.TRUE.equals(slotState.get("isCasting"))) {
                        slotState.put("remainingCooldownMs", 0L);
                        slotState.put("isReady", Boolean.TRUE);
                    }
                    continue;
                }
                long nextRemaining = Math.max(0L, remainingCooldownMs - deltaMs);
                slotState.put("remainingCooldownMs", nextRemaining);
                if (nextRemaining <= 0L && !Boolean.TRUE.equals(slotState.get("isCasting"))) {
                    slotState.put("isReady", Boolean.TRUE);
                } else if (nextRemaining > 0L) {
                    slotState.put("isReady", Boolean.FALSE);
                }
            }
        }
    }

    private void tickHealthRelics(BattleRoom room, long now) {
        if (room == null || room.getHealthRelics() == null || room.getHealthRelics().isEmpty()) {
            return;
        }
        for (BattleHealthRelicState relic : room.getHealthRelics()) {
            if (relic == null || relic.getPosition() == null) {
                continue;
            }
            if (!Boolean.TRUE.equals(relic.getIsAvailable()) && relic.getRespawnAt() != null && now >= relic.getRespawnAt()) {
                relic.setIsAvailable(Boolean.TRUE);
                relic.setRespawnAt(null);
            }
            if (!Boolean.TRUE.equals(relic.getIsAvailable())) {
                continue;
            }
            double pickupRadius = relic.getPickupRadius() != null ? relic.getPickupRadius() : 2.5D;
            for (BattleChampionState champion : room.getChampions()) {
                if (champion == null || champion.getPosition() == null || Boolean.TRUE.equals(champion.getDead())) {
                    continue;
                }
                if (champion.getHp() == null || champion.getMaxHp() == null || champion.getHp() >= champion.getMaxHp()) {
                    continue;
                }
                double dx = champion.getPosition().getX() - relic.getPosition().getX();
                double dz = champion.getPosition().getZ() - relic.getPosition().getZ();
                if (dx * dx + dz * dz > pickupRadius * pickupRadius) {
                    continue;
                }
                double healAmount = champion.getMaxHp() * (relic.getHealPercent() != null ? relic.getHealPercent() : 0.15D);
                effectAtomicExecutor.applyHeal(relic.getId(), null, "health_relic", null, champion, healAmount);
                relic.setIsAvailable(Boolean.FALSE);
                relic.setRespawnAt(now + resolveHealthRelicRespawnMs(relic.getId()));

                Map<String, Object> fields = new LinkedHashMap<String, Object>();
                fields.put("relicId", relic.getId());
                fields.put("championId", champion.getId());
                fields.put("healAmount", healAmount);
                battleBroadcastService.broadcastCombatEvent(room, "RelicPickup", "relic-pickup", now, fields);
                break;
            }
        }
    }

    private long resolveHealthRelicRespawnMs(String relicId) {
        JsonNode root = battleRoomManager.getLatestMapConfigRoot();
        if (root == null || root.isMissingNode()) {
            return 30000L;
        }
        JsonNode items = root.path("healthRelics").path("items");
        if (!items.isArray()) {
            return 30000L;
        }
        for (JsonNode item : items) {
            if (item == null || !item.isObject()) {
                continue;
            }
            if (relicId != null && relicId.equals(item.path("id").asText(null))) {
                return Math.max(1000L, item.path("respawnMs").asLong(30000L));
            }
        }
        JsonNode first = items.size() > 0 ? items.get(0) : null;
        return Math.max(1000L, first != null ? first.path("respawnMs").asLong(30000L) : 30000L);
    }

    // ==================== 小兵 AI ====================

    private void tickMinions(BattleRoom room, long now, double deltaSeconds, TickComputationContext context) {
        if (room == null) {
            return;
        }
        if (!Boolean.FALSE.equals(fishBattleServerProperties.getSpawnMinionsEnabled())) {
            spawnMinionWaveIfNeeded(room, now);
        }
        if (room.getMinions() == null || room.getMinions().isEmpty()) {
            return;
        }
        room.getMinions().removeIf(m ->
                m != null && Boolean.TRUE.equals(m.getDead())
                        && m.getDeadAt() != null && (now - m.getDeadAt()) > MINION_DEATH_CLEANUP_MS);

        for (BattleMinionState minion : room.getMinions()) {
            if (minion == null || Boolean.TRUE.equals(minion.getDead()) || minion.getPosition() == null) {
                continue;
            }
            Object target = findMinionTarget(room, minion, context);
            tickMinionCombat(room, minion, target, now, deltaSeconds);
        }
        resolveMinionSeparation(room);
    }

    private Object findMinionTarget(BattleRoom room, BattleMinionState minion, TickComputationContext context) {
        double acquisitionRange = minion.getAcquisitionRange() != null ? minion.getAcquisitionRange() : 5.5D;
        BattleMinionState enemyMinion = findNearestEnemyMinion(room, minion, acquisitionRange);
        if (enemyMinion != null) {
            return enemyMinion;
        }
        BattleChampionState enemyChampion = findNearestEnemyChampion(room, minion, acquisitionRange, context);
        if (enemyChampion != null) {
            return enemyChampion;
        }
        return findNearestEnemyStructure(room, minion, acquisitionRange + 4D);
    }

    private BattleMinionState findNearestEnemyMinion(BattleRoom room, BattleMinionState source, double maxRange) {
        BattleMinionState nearest = null;
        double nearestDistSq = maxRange * maxRange;
        for (BattleMinionState candidate : room.getMinions()) {
            if (candidate == null || candidate == source || Boolean.TRUE.equals(candidate.getDead())) {
                continue;
            }
            if (source.getTeam() != null && source.getTeam().equals(candidate.getTeam())) {
                continue;
            }
            double distSq = distanceSq(source.getPosition(), candidate.getPosition());
            if (distSq <= nearestDistSq) {
                nearest = candidate;
                nearestDistSq = distSq;
            }
        }
        return nearest;
    }

    private BattleChampionState findNearestEnemyChampion(BattleRoom room, BattleMinionState source, double maxRange, TickComputationContext context) {
        BattleChampionState nearest = null;
        double nearestDistSq = maxRange * maxRange;
        Set<String> allyBushIds = resolveTeamBushIds(room, source.getTeam(), context);
        for (BattleChampionState candidate : room.getChampions()) {
            if (candidate == null || candidate.getPosition() == null || Boolean.TRUE.equals(candidate.getDead())) {
                continue;
            }
            if (source.getTeam() != null && source.getTeam().equals(candidate.getTeam())) {
                continue;
            }
            String candidateBush = resolveChampionBushId(candidate, context);
            if (candidateBush != null && !allyBushIds.contains(candidateBush)) {
                continue;
            }
            double distSq = distanceSq(source.getPosition(), candidate.getPosition());
            if (distSq <= nearestDistSq) {
                nearest = candidate;
                nearestDistSq = distSq;
            }
        }
        return nearest;
    }

    private BattleStructureState findNearestEnemyStructure(BattleRoom room, BattleMinionState source, double maxRange) {
        String enemyTeam = "blue".equals(source.getTeam()) ? "red" : "blue";
        double maxRangeSq = maxRange * maxRange;

        for (String[] tier : STRUCTURE_ATTACK_ORDER) {
            String reqType = tier[0];
            String reqSubType = tier[1];
            boolean allDestroyedInTier = true;
            BattleStructureState nearest = null;
            double nearestDistSq = maxRangeSq;

            for (BattleStructureState s : room.getStructures()) {
                if (s == null || s.getPosition() == null) continue;
                if (!enemyTeam.equals(s.getTeam())) continue;
                if (!reqType.equals(s.getType())) continue;
                if (reqSubType != null && !reqSubType.equals(s.getSubType())) continue;
                if (!Boolean.TRUE.equals(s.getIsDestroyed())) {
                    allDestroyedInTier = false;
                    double distSq = distanceSq(source.getPosition(), s.getPosition());
                    if (distSq <= nearestDistSq) {
                        nearest = s;
                        nearestDistSq = distSq;
                    }
                }
            }
            if (nearest != null) {
                return nearest;
            }
            if (!allDestroyedInTier) {
                return null;
            }
        }
        return null;
    }

    private void tickMinionCombat(BattleRoom room, BattleMinionState minion, Object target, long now, double deltaSeconds) {
        BattleVector3 targetPosition = resolveTargetPosition(target);
        if (targetPosition == null) {
            tickMinionAdvance(minion, deltaSeconds);
            return;
        }
        minion.setTargetEntityId(resolveTargetId(target));
        minion.setTargetType(resolveTargetType(target));
        double dx = targetPosition.getX() - minion.getPosition().getX();
        double dz = targetPosition.getZ() - minion.getPosition().getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        double attackRange = (minion.getAttackRange() != null ? minion.getAttackRange() : 1.35D)
                + resolveCollisionRadius(target);
        if (distance <= attackRange) {
            minion.setAnimationState("attack");
            faceTowards(minion, targetPosition);
            long attackCooldownMs = minion.getAttackCooldownMs() != null ? minion.getAttackCooldownMs() : 1200L;
            long lastAttackAt = minion.getLastAttackAt() != null ? minion.getLastAttackAt() : 0L;
            if (now - lastAttackAt >= attackCooldownMs) {
                applyMinionAttack(room, minion, target, now);
                minion.setLastAttackAt(now);
            }
            return;
        }
        moveMinionTowards(minion, targetPosition, deltaSeconds);
        minion.setAnimationState("run");
    }

    private void tickMinionAdvance(BattleMinionState minion, double deltaSeconds) {
        if (minion == null || minion.getPosition() == null) {
            return;
        }
        double destX = "blue".equals(minion.getTeam()) ? 115D : -110D;
        double curZ = minion.getPosition().getZ();
        double targetZ = curZ * (1.0D - MINION_Z_CONVERGE_FACTOR);
        if (Math.abs(targetZ) < 0.3D) {
            targetZ = 0D;
        }
        BattleVector3 target = BattleVector3.builder().x(destX).y(0D).z(targetZ).build();
        minion.setTargetEntityId(null);
        minion.setTargetType(null);
        moveMinionTowards(minion, target, deltaSeconds);
        minion.setAnimationState("run");
    }

    private void resolveMinionSeparation(BattleRoom room) {
        List<BattleMinionState> minions = room.getMinions();
        int size = minions.size();
        for (int i = 0; i < size; i++) {
            BattleMinionState a = minions.get(i);
            if (a == null || Boolean.TRUE.equals(a.getDead()) || a.getPosition() == null) {
                continue;
            }
            double radiusA = a.getCollisionRadius() != null ? a.getCollisionRadius() : 0.45D;
            for (int j = i + 1; j < size; j++) {
                BattleMinionState b = minions.get(j);
                if (b == null || Boolean.TRUE.equals(b.getDead()) || b.getPosition() == null) {
                    continue;
                }
                double radiusB = b.getCollisionRadius() != null ? b.getCollisionRadius() : 0.45D;
                double ddx = a.getPosition().getX() - b.getPosition().getX();
                double ddz = a.getPosition().getZ() - b.getPosition().getZ();
                double distSq = ddx * ddx + ddz * ddz;
                double minDist = radiusA + radiusB;
                if (distSq < minDist * minDist && distSq > 1e-8) {
                    double dist = Math.sqrt(distSq);
                    double overlap = minDist - dist;
                    double pushX = ddx / dist * overlap * 0.5D;
                    double pushZ = ddz / dist * overlap * 0.5D;
                    a.getPosition().setX(a.getPosition().getX() + pushX);
                    a.getPosition().setZ(a.getPosition().getZ() + pushZ);
                    b.getPosition().setX(b.getPosition().getX() - pushX);
                    b.getPosition().setZ(b.getPosition().getZ() - pushZ);
                } else if (distSq <= 1e-8) {
                    a.getPosition().setX(a.getPosition().getX() + 0.3D);
                    b.getPosition().setX(b.getPosition().getX() - 0.3D);
                }
            }
        }
    }

    private void moveMinionTowards(BattleMinionState minion, BattleVector3 targetPosition, double deltaSeconds) {
        double dx = targetPosition.getX() - minion.getPosition().getX();
        double dz = targetPosition.getZ() - minion.getPosition().getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance <= MINION_REACHED_TARGET_EPSILON) {
            return;
        }
        double dirX = dx / distance;
        double dirZ = dz / distance;
        double speed = minion.getMoveSpeed() != null ? minion.getMoveSpeed() : 2.4D;
        double step = Math.min(distance, speed * deltaSeconds);
        minion.getPosition().setX(minion.getPosition().getX() + dirX * step);
        minion.getPosition().setZ(minion.getPosition().getZ() + dirZ * step);
        faceTowards(minion, targetPosition);
        resolveMinionStructureCollision(minion.getPosition(), minion.getTeam(), dirX, dirZ);
    }

    private void applyMinionAttack(BattleRoom room, BattleMinionState minion, Object target, long now) {
        double damage = minion.getAttackDamage() != null ? minion.getAttackDamage() : 42D;
        String targetId = resolveTargetId(target);
        String targetType = resolveTargetType(target);
        if (targetId == null || targetType == null) {
            return;
        }
        if (target instanceof BattleStructureState && isStructureProtected(room, (BattleStructureState) target)) {
            return;
        }
        BattleVector3 attackerPosition = minion.getPosition();
        BattleVector3 targetPosition = resolveTargetPosition(target);
        boolean isCasterMinion = "caster".equalsIgnoreCase(minion.getMinionType());
        long impactDelayMs = isCasterMinion
                ? computeProjectileFlightDelayMs(attackerPosition, targetPosition, MINION_CASTER_PROJECTILE_SPEED, 120L)
                : MINION_MELEE_WINDUP_MS;

        enqueuePendingAttack(room, PendingAttackState.builder()
                .sourceEntityId(minion.getId())
                .sourceType("minion")
                .targetEntityId(targetId)
                .targetType(targetType)
                .skillId("minion_basic")
                .damage(damage)
                .damageType("physical")
                .impactAt(now + impactDelayMs)
                .build());

        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        fields.put("minionId", minion.getId());
        fields.put("minionType", minion.getMinionType());
        fields.put("targetId", targetId);
        fields.put("targetType", targetType);
        fields.put("damage", damage);
        fields.put("attackerPosition", minion.getPosition());
        fields.put("impactDelayMs", impactDelayMs);
        battleBroadcastService.broadcastCombatEvent(room, "minionAttack", "minion-atk", now, fields);
    }

    private void faceTowards(BattleMinionState minion, BattleVector3 targetPosition) {
        if (minion == null || minion.getPosition() == null || targetPosition == null) {
            return;
        }
        double dx = targetPosition.getX() - minion.getPosition().getX();
        double dz = targetPosition.getZ() - minion.getPosition().getZ();
        if (Math.abs(dx) < 0.001D && Math.abs(dz) < 0.001D) {
            return;
        }
        minion.setRotation(Math.atan2(dx, dz));
    }

    private void resolveMinionStructureCollision(BattleVector3 pos, String minionTeam,
                                                  double moveDirX, double moveDirZ) {
        if (structureColliders == null) return;
        double minionRadius = 0.45;
        boolean hasMoveDir = (moveDirX * moveDirX + moveDirZ * moveDirZ) > 0.01;
        for (int i = 0; i < structureColliders.length; i++) {
            double[] collider = structureColliders[i];
            double cx = collider[0];
            double cz = collider[1];
            double structureRadius = collider[2];

            boolean friendly = minionTeam != null && minionTeam.equals(structureColliderTeams[i]);
            double effectiveStructureR = friendly ? structureRadius * 0.55D : structureRadius;
            double minDist = effectiveStructureR + minionRadius;

            double dx = pos.getX() - cx;
            double dz = pos.getZ() - cz;
            double distSq = dx * dx + dz * dz;
            double minDistSq = minDist * minDist;

            if (distSq < minDistSq && distSq > 1e-8) {
                double dist = Math.sqrt(distSq);
                double pushFactor = minDist / dist;
                pos.setX(cx + dx * pushFactor);
                pos.setZ(cz + dz * pushFactor);

                if (hasMoveDir) {
                    double nx = dx / dist;
                    double nz = dz / dist;
                    double tangentX = -nz;
                    double tangentZ = nx;
                    double dot = tangentX * moveDirX + tangentZ * moveDirZ;
                    if (dot < 0) {
                        tangentX = -tangentX;
                        tangentZ = -tangentZ;
                    }
                    pos.setX(pos.getX() + tangentX * 0.1D);
                    pos.setZ(pos.getZ() + tangentZ * 0.1D);
                }
            } else if (distSq <= 1e-8) {
                pos.setX(cx + minDist);
            }
        }
    }

    // ==================== 防御塔 AI ====================

    /**
     * 防御塔 AI：
     * 优先级 1 - 近 6 秒攻击过己方英雄的敌方英雄（仇恨）
     * 优先级 2 - 最近的敌方小兵
     * 优先级 3 - 最近的敌方英雄
     */
    private void tickTowers(BattleRoom room, long now, TickComputationContext context) {
        if (room == null || room.getStructures() == null) {
            return;
        }
        for (BattleStructureState tower : room.getStructures()) {
            if (tower == null || !"tower".equals(tower.getType()) || Boolean.TRUE.equals(tower.getIsDestroyed())) {
                continue;
            }
            double attackDamage = tower.getAttackDamage() != null ? tower.getAttackDamage() : 0D;
            double attackRange = tower.getAttackRange() != null ? tower.getAttackRange() : 0D;
            double attackSpeed = tower.getAttackSpeed() != null ? tower.getAttackSpeed() : 0D;
            if (attackDamage <= 0D || attackRange <= 0D || attackSpeed <= 0D) {
                continue;
            }
            long attackCooldownMs = (long) (1000D / attackSpeed);
            String enemyTeam = "blue".equals(tower.getTeam()) ? "red" : "blue";
            double rangeSq = attackRange * attackRange;

            Object target = findTowerTarget(room, tower, enemyTeam, rangeSq, context);
            if (target != null) {
                tower.setTargetEntityId(resolveTargetId(target));
                long lastAtk = tower.getLastAttackAt() != null ? tower.getLastAttackAt() : 0L;
                if (now - lastAtk >= attackCooldownMs) {
                    applyTowerAttack(room, tower, target, now);
                    tower.setLastAttackAt(now);
                }
            } else {
                tower.setTargetEntityId(null);
            }
        }
    }

    private Object findTowerTarget(BattleRoom room, BattleStructureState tower,
                                    String enemyTeam, double rangeSq, TickComputationContext context) {
        Set<String> allyBushIds = resolveTeamBushIds(room, tower.getTeam(), context);

        /* P1: 近 6 秒攻击过己方英雄的敌方英雄 */
        BattleChampionState aggroChamp = null;
        double aggroDistSq = rangeSq;
        for (BattleChampionState c : room.getChampions()) {
            if (c == null || c.getPosition() == null || Boolean.TRUE.equals(c.getDead())) continue;
            if (!enemyTeam.equals(c.getTeam())) continue;
            String cBush = resolveChampionBushId(c, context);
            if (cBush != null && !allyBushIds.contains(cBush)) continue;
            Long lastAttackedAt = c.getLastAttackedEnemyChampionAt();
            if (lastAttackedAt == null || (System.currentTimeMillis() - lastAttackedAt) > 6000L) continue;
            double dSq = distanceSq(tower.getPosition(), c.getPosition());
            if (dSq <= aggroDistSq) {
                aggroChamp = c;
                aggroDistSq = dSq;
            }
        }
        if (aggroChamp != null) return aggroChamp;

        /* P2: 最近的敌方小兵 */
        BattleMinionState nearestMinion = null;
        double nearestMinionDistSq = rangeSq;
        for (BattleMinionState m : room.getMinions()) {
            if (m == null || Boolean.TRUE.equals(m.getDead()) || m.getPosition() == null) continue;
            if (!enemyTeam.equals(m.getTeam())) continue;
            double dSq = distanceSq(tower.getPosition(), m.getPosition());
            if (dSq <= nearestMinionDistSq) {
                nearestMinion = m;
                nearestMinionDistSq = dSq;
            }
        }
        if (nearestMinion != null) return nearestMinion;

        /* P3: 最近的敌方英雄 */
        BattleChampionState nearestChamp = null;
        double nearestChampDistSq = rangeSq;
        for (BattleChampionState c : room.getChampions()) {
            if (c == null || c.getPosition() == null || Boolean.TRUE.equals(c.getDead())) continue;
            if (!enemyTeam.equals(c.getTeam())) continue;
            String cBush = resolveChampionBushId(c, context);
            if (cBush != null && !allyBushIds.contains(cBush)) continue;
            double dSq = distanceSq(tower.getPosition(), c.getPosition());
            if (dSq <= nearestChampDistSq) {
                nearestChamp = c;
                nearestChampDistSq = dSq;
            }
        }
        return nearestChamp;
    }

    private void applyTowerAttack(BattleRoom room, BattleStructureState tower, Object target, long now) {
        double damage = tower.getAttackDamage() != null ? tower.getAttackDamage() : 150D;
        String targetId = resolveTargetId(target);
        String targetType = resolveTargetType(target);
        if (targetId == null || targetType == null) return;

        BattleVector3 targetPosition = resolveTargetPosition(target);
        long impactDelayMs = computeProjectileFlightDelayMs(tower.getPosition(), targetPosition, TOWER_PROJECTILE_SPEED, 100L);

        enqueuePendingAttack(room, PendingAttackState.builder()
                .sourceEntityId(tower.getId())
                .sourceType("structure")
                .targetEntityId(targetId)
                .targetType(targetType)
                .skillId("tower_basic")
                .damage(damage)
                .damageType("physical")
                .impactAt(now + impactDelayMs)
                .build());

        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        fields.put("towerId", tower.getId());
        fields.put("targetId", targetId);
        fields.put("targetType", targetType);
        fields.put("damage", damage);
        fields.put("towerPosition", tower.getPosition());
        fields.put("impactDelayMs", impactDelayMs);
        battleBroadcastService.broadcastCombatEvent(room, "towerAttack", "tower-atk", now, fields);
    }

    // ==================== 延迟伤害管线 ====================

    private void enqueuePendingAttack(BattleRoom room, PendingAttackState pendingAttack) {
        if (room == null || pendingAttack == null) {
            return;
        }
        if (room.getPendingAttacks() == null) {
            room.setPendingAttacks(new CopyOnWriteArrayList<PendingAttackState>());
        }
        room.getPendingAttacks().add(pendingAttack);
    }

    private long computeProjectileFlightDelayMs(BattleVector3 sourcePosition, BattleVector3 targetPosition,
                                                double speed, long minimumDelayMs) {
        if (sourcePosition == null || targetPosition == null || speed <= 0D) {
            return minimumDelayMs;
        }
        double distance = Math.sqrt(distanceSq(sourcePosition, targetPosition));
        return Math.max(minimumDelayMs, (long) ((distance / speed) * 1000D));
    }

    private void resolvePendingAttacks(BattleRoom room, long now) {
        if (room == null || room.getPendingAttacks() == null || room.getPendingAttacks().isEmpty()) {
            return;
        }
        List<PendingAttackState> resolvedAttacks = new ArrayList<PendingAttackState>();
        for (PendingAttackState pendingAttack : room.getPendingAttacks()) {
            if (pendingAttack == null) {
                resolvedAttacks.add(null);
                continue;
            }
            Long impactAt = pendingAttack.getImpactAt();
            if (impactAt != null && impactAt > now) {
                continue;
            }
            applyPendingAttack(room, pendingAttack, now);
            resolvedAttacks.add(pendingAttack);
        }
        if (!resolvedAttacks.isEmpty()) {
            room.getPendingAttacks().removeAll(resolvedAttacks);
        }
    }

    private void applyPendingAttack(BattleRoom room, PendingAttackState pendingAttack, long now) {
        if (room == null || pendingAttack == null || pendingAttack.getTargetEntityId() == null) {
            return;
        }
        double damage = pendingAttack.getDamage() != null ? pendingAttack.getDamage() : 0D;
        if (damage <= 0D) {
            return;
        }
        String targetType = pendingAttack.getTargetType();
        if ("champion".equals(targetType)) {
            BattleChampionState champion = findChampionById(room, pendingAttack.getTargetEntityId());
            if (champion == null || Boolean.TRUE.equals(champion.getDead())) {
                return;
            }
            effectAtomicExecutor.applyDamage(
                    pendingAttack.getSourceEntityId(),
                    null,
                    pendingAttack.getSkillId(),
                    null,
                    champion,
                    damage,
                    pendingAttack.getDamageType() != null ? pendingAttack.getDamageType() : "physical"
            );
            return;
        }
        if ("minion".equals(targetType)) {
            BattleMinionState minion = findMinionById(room, pendingAttack.getTargetEntityId());
            if (minion == null || Boolean.TRUE.equals(minion.getDead()) || minion.getHp() == null) {
                return;
            }
            minion.setHp(Math.max(0D, minion.getHp() - damage));
            if (minion.getHp() <= 0D) {
                minion.setDead(Boolean.TRUE);
                minion.setDeadAt(now);
                minion.setAnimationState("death");
            }
            return;
        }
        if ("structure".equals(targetType)) {
            BattleStructureState structure = findStructureById(room, pendingAttack.getTargetEntityId());
            if (structure == null || Boolean.TRUE.equals(structure.getIsDestroyed())) {
                return;
            }
            if ("minion".equals(pendingAttack.getSourceType()) && isStructureProtected(room, structure)) {
                return;
            }
            effectAtomicExecutor.applyDamageToStructure(
                    pendingAttack.getSourceEntityId(),
                    null,
                    pendingAttack.getSkillId(),
                    null,
                    structure,
                    damage
            );
        }
    }

    // ==================== 建筑保护链 ====================

    private boolean isStructureProtected(BattleRoom room, BattleStructureState structure) {
        if (structure == null) return false;
        String team = structure.getTeam();
        if (team == null) return false;
        String sType = structure.getType();
        String sSubType = structure.getSubType();

        for (String[] tier : STRUCTURE_ATTACK_ORDER) {
            String reqType = tier[0];
            String reqSubType = tier[1];
            boolean matchesTier = reqType.equals(sType) && (reqSubType == null || reqSubType.equals(sSubType));
            if (matchesTier) {
                return false;
            }
            for (BattleStructureState s : room.getStructures()) {
                if (s == null) continue;
                if (!team.equals(s.getTeam())) continue;
                if (!reqType.equals(s.getType())) continue;
                if (reqSubType != null && !reqSubType.equals(s.getSubType())) continue;
                if (!Boolean.TRUE.equals(s.getIsDestroyed())) {
                    return true;
                }
            }
        }
        return false;
    }

    // ==================== 目标解析辅助 ====================

    private BattleVector3 resolveTargetPosition(Object target) {
        if (target instanceof BattleMinionState) {
            return ((BattleMinionState) target).getPosition();
        }
        if (target instanceof BattleChampionState) {
            return ((BattleChampionState) target).getPosition();
        }
        if (target instanceof BattleStructureState) {
            return ((BattleStructureState) target).getPosition();
        }
        return null;
    }

    private String resolveTargetId(Object target) {
        if (target instanceof BattleMinionState) {
            return ((BattleMinionState) target).getId();
        }
        if (target instanceof BattleChampionState) {
            return ((BattleChampionState) target).getId();
        }
        if (target instanceof BattleStructureState) {
            return ((BattleStructureState) target).getId();
        }
        return null;
    }

    private String resolveTargetType(Object target) {
        if (target instanceof BattleMinionState) {
            return "minion";
        }
        if (target instanceof BattleChampionState) {
            return "champion";
        }
        if (target instanceof BattleStructureState) {
            return "structure";
        }
        return null;
    }

    private double resolveCollisionRadius(Object target) {
        if (target instanceof BattleMinionState) {
            return ((BattleMinionState) target).getCollisionRadius() != null
                    ? ((BattleMinionState) target).getCollisionRadius() : 0.45D;
        }
        if (target instanceof BattleChampionState) {
            return CHAMPION_COLLISION_RADIUS;
        }
        if (target instanceof BattleStructureState) {
            return ((BattleStructureState) target).getCollisionRadius() != null
                    ? ((BattleStructureState) target).getCollisionRadius() : 0D;
        }
        return 0D;
    }

    private BattleChampionState findChampionById(BattleRoom room, String championId) {
        if (room == null || room.getChampions() == null || championId == null) return null;
        for (BattleChampionState c : room.getChampions()) {
            if (c != null && championId.equals(c.getId())) return c;
        }
        return null;
    }

    private BattleMinionState findMinionById(BattleRoom room, String minionId) {
        if (room == null || room.getMinions() == null || minionId == null) return null;
        for (BattleMinionState m : room.getMinions()) {
            if (m != null && minionId.equals(m.getId())) return m;
        }
        return null;
    }

    private BattleStructureState findStructureById(BattleRoom room, String structureId) {
        if (room == null || room.getStructures() == null || structureId == null) return null;
        for (BattleStructureState s : room.getStructures()) {
            if (s != null && structureId.equals(s.getId())) return s;
        }
        return null;
    }

    private double distanceSq(BattleVector3 a, BattleVector3 b) {
        if (a == null || b == null) return Double.MAX_VALUE;
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    // ==================== 小兵出兵 ====================

    private void spawnMinionWaveIfNeeded(BattleRoom room, long now) {
        long nextSpawnAt = room.getNextMinionSpawnAt() != null ? room.getNextMinionSpawnAt() : 0L;
        if (nextSpawnAt > now) {
            return;
        }
        room.setNextMinionSpawnAt(now + Math.max(1000L, minionSpawnIntervalMs));
        long waveSequence = (room.getMinionWaveSequence() != null ? room.getMinionWaveSequence() : 0L) + 1L;
        room.setMinionWaveSequence(waveSequence);

        String onlyTeam = fishBattleServerProperties.getSpawnOnlyTeam();
        if (onlyTeam == null || "blue".equals(onlyTeam)) {
            appendWaveForTeam(room, now, waveSequence, "blue");
        }
        if (onlyTeam == null || "red".equals(onlyTeam)) {
            appendWaveForTeam(room, now, waveSequence, "red");
        }
    }

    private void appendWaveForTeam(BattleRoom room, long now, long waveSequence, String team) {
        JsonNode minionRoot = resolveMinionConfigRoot();
        JsonNode zOffsetsNode = minionRoot.path("zOffsets");
        double[] zOffsets = zOffsetsNode.isArray() && zOffsetsNode.size() > 0
                ? new double[zOffsetsNode.size()]
                : new double[]{-2D, 0D, 2D};
        for (int i = 0; i < zOffsetsNode.size(); i++) {
            zOffsets[i] = zOffsetsNode.get(i).asDouble(0D);
        }
        if (zOffsetsNode.size() == 0) {
            zOffsets = new double[]{-2D, 0D, 2D};
        }
        for (int i = 0; i < zOffsets.length; i++) {
            room.getMinions().add(buildMinionState(now, waveSequence, team, "melee", i, zOffsets[i], minionRoot));
        }
        room.getMinions().add(buildMinionState(now, waveSequence, team, "caster", zOffsets.length, 0D, minionRoot));
    }

    private BattleMinionState buildMinionState(long now, long waveSequence, String team, String minionType,
                                               int index, double zOffset, JsonNode minionRoot) {
        JsonNode typeRoot = minionRoot.path(minionType);
        JsonNode spawnPointRoot = minionRoot.path("spawnPoints").path(team);
        double x = "caster".equals(minionType)
                ? spawnPointRoot.path("casterX").asDouble("blue".equals(team) ? -107D : 107D)
                : spawnPointRoot.path("meleeX").asDouble("blue".equals(team) ? -105D : 105D);
        String modelUrl = typeRoot.path("modelUrl").path(team).asText(null);
        double hp = typeRoot.path("hp").asDouble(300D);
        return BattleMinionState.builder()
                .id(team + "_minion_" + waveSequence + "_" + minionType + "_" + index)
                .team(team)
                .lane("mid")
                .minionType(minionType)
                .position(BattleVector3.builder().x(x).y(0D).z(zOffset).build())
                .rotation("blue".equals(team) ? Math.PI / 2D : -Math.PI / 2D)
                .moveSpeed(typeRoot.path("moveSpeed").asDouble(2.4D))
                .collisionRadius(typeRoot.path("collisionRadius").asDouble(0.4D))
                .hp(hp)
                .maxHp(hp)
                .attackDamage(typeRoot.path("attackDamage").asDouble(0D))
                .attackRange(typeRoot.path("attackRange").asDouble(0D))
                .acquisitionRange(typeRoot.path("acquisitionRange").asDouble(0D))
                .attackCooldownMs(typeRoot.path("attackCooldownMs").asLong(1000L))
                .lastAttackAt(now)
                .dead(Boolean.FALSE)
                .deadAt(0L)
                .animationState("run")
                .targetEntityId(null)
                .targetType(null)
                .modelUrl(modelUrl)
                .build();
    }

    private JsonNode resolveMinionConfigRoot() {
        JsonNode root = battleRoomManager.getLatestMapConfigRoot();
        if (root == null || root.isMissingNode()) {
            return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
        }
        return root.path("minions");
    }

    private double resolveEffectiveMoveSpeed(BattleRoom room, BattleChampionState champion) {
        double baseMoveSpeed = champion.getBaseMoveSpeed() != null ? champion.getBaseMoveSpeed() : (champion.getMoveSpeed() != null ? champion.getMoveSpeed() : 3D);
        double multiplier = 1D;
        if (statusEffectService.hasStatus(room.getRoomId(), champion.getId(), "summoner_ghost_speed_up")) {
            multiplier *= 1.45D;
        }
        if (statusEffectService.hasStatus(room.getRoomId(), champion.getId(), "generic_move_speed_up")) {
            multiplier *= 1.15D;
        }
        return baseMoveSpeed * multiplier;
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

    // ==================== 快照广播 ====================

    private void emitSnapshot(BattleRoom room, long now, TickComputationContext context) {
        Map<String, Object> snapshotBase = new LinkedHashMap<String, Object>();
        snapshotBase.put("eventId", "snapshot-" + room.getSequence().incrementAndGet());
        snapshotBase.put("sequence", room.getSequence().get());
        snapshotBase.put("roomId", room.getRoomId());
        snapshotBase.put("serverTime", now);
        snapshotBase.put("serverTick", room.getTickNumber());
        snapshotBase.put("gameTimer", room.getGameTimer());

        List<Map<String, Object>> championList = buildChampionSnapshotEntities(room);
        List<Map<String, Object>> minionList = buildMinionSnapshotEntities(room);
        Map<String, List<Map<String, Object>>> structureSnapshot = buildStructureSnapshotEntities(room);
        List<Map<String, Object>> healthRelicList = buildHealthRelicSnapshotEntities(room);
        List<Map<String, Object>> playerList = buildPlayerSnapshotEntities(room);

        if (room.getPlayers() == null || room.getPlayers().isEmpty()) {
            return;
        }

        // 构建 championId → team 映射，用于查询玩家所属队伍
        Map<String, String> champTeamMap = new HashMap<String, String>();
        for (BattleChampionState c : room.getChampions()) {
            if (c != null) champTeamMap.put(c.getId(), c.getTeam());
        }

        for (PlayerSession playerSession : room.getPlayers()) {
            Map<String, Object> snapshot = new LinkedHashMap<String, Object>(snapshotBase);

            // 按玩家视角过滤草丛中的敌方英雄
            boolean isSpectator = Boolean.TRUE.equals(playerSession.getSpectator());
            String viewerTeam = champTeamMap.getOrDefault(playerSession.getChampionId(), "");
            List<Map<String, Object>> visibleChampions =
                    isSpectator || viewerTeam.isEmpty()
                            ? championList
                            : filterChampionsByVision(championList, room, viewerTeam);

            snapshot.put("champions", visibleChampions);
            snapshot.put("minions", minionList);
            snapshot.put("towers", structureSnapshot.get("towers"));
            snapshot.put("nexuses", structureSnapshot.get("nexuses"));
            snapshot.put("inhibitors", structureSnapshot.get("inhibitors"));
            snapshot.put("healthRelics", healthRelicList);
            snapshot.put("players", playerList);
            battleBroadcastService.sendToPlayer(playerSession, "combatSnapshot", snapshot);
        }
    }

    private List<Map<String, Object>> buildHealthRelicSnapshotEntities(BattleRoom room) {
        List<Map<String, Object>> relicList = new ArrayList<Map<String, Object>>();
        if (room == null || room.getHealthRelics() == null) {
            return relicList;
        }
        for (BattleHealthRelicState relic : room.getHealthRelics()) {
            if (relic == null) {
                continue;
            }
            Map<String, Object> relicMap = new LinkedHashMap<String, Object>();
            relicMap.put("id", relic.getId());
            relicMap.put("position", relic.getPosition());
            relicMap.put("isAvailable", relic.getIsAvailable());
            relicMap.put("respawnAt", relic.getRespawnAt());
            relicList.add(relicMap);
        }
        return relicList;
    }

    private List<Map<String, Object>> buildChampionSnapshotEntities(BattleRoom room) {
        List<Map<String, Object>> entityList = new ArrayList<Map<String, Object>>();
        if (room == null || room.getChampions() == null) {
            return entityList;
        }
        for (BattleChampionState champion : room.getChampions()) {
            entityList.add(buildChampionSnapshotEntity(champion));
        }
        return entityList;
    }

    private Map<String, Object> buildChampionSnapshotEntity(BattleChampionState champion) {
        if (champion == null) {
            return new LinkedHashMap<String, Object>();
        }
        Map<String, Object> entity = new LinkedHashMap<String, Object>();
        entity.put("id", champion.getId());
        entity.put("heroId", champion.getHeroId());
        entity.put("skin", champion.getSkin());
        entity.put("modelUrl", champion.getModelUrl());
        entity.put("playerName", champion.getPlayerName());
        entity.put("team", champion.getTeam());
        entity.put("position", champion.getPosition());
        entity.put("rotation", champion.getRotation());
        entity.put("moveSpeed", champion.getMoveSpeed());
        entity.put("moveTarget", champion.getMoveTarget());
        entity.put("animationState", champion.getAnimationState());
        entity.put("inputMode", champion.getInputMode());
        entity.put("dead", champion.getDead());
        entity.put("isDead", champion.getDead() != null ? champion.getDead() : false);
        entity.put("hp", champion.getHp());
        entity.put("maxHp", champion.getMaxHp());
        entity.put("mp", champion.getMp());
        entity.put("maxMp", champion.getMaxMp());
        entity.put("shield", champion.getShield());
        entity.put("flowValue", champion.getFlowValue());
        entity.put("skillStates", champion.getSkillStates());
        entity.put("activeCastInstanceId", champion.getActiveCastInstanceId());
        entity.put("activeCastPhase", champion.getActiveCastPhase());
        entity.put("movementLockedUntil", champion.getMovementLockedUntil());
        entity.put("idleStartedAt", champion.getIdleStartedAt());
        entity.put("lastProcessedMoveSequence", champion.getLastProcessedMoveSequence());
        entity.put("lastMoveCommandClientTime", champion.getLastMoveCommandClientTime());
        entity.put("lastMoveCommandServerTime", champion.getLastMoveCommandServerTime());
        entity.put("lastProcessedInputSeq", champion.getLastProcessedInputSeq());
        return entity;
    }

    private List<Map<String, Object>> buildMinionSnapshotEntities(BattleRoom room) {
        List<Map<String, Object>> entityList = new ArrayList<Map<String, Object>>();
        if (room == null || room.getMinions() == null) {
            return entityList;
        }
        for (BattleMinionState minion : room.getMinions()) {
            if (minion == null) {
                continue;
            }
            Map<String, Object> entity = new LinkedHashMap<String, Object>();
            entity.put("id", minion.getId());
            entity.put("team", minion.getTeam());
            entity.put("lane", minion.getLane());
            entity.put("minionType", minion.getMinionType());
            entity.put("position", minion.getPosition());
            entity.put("rotation", minion.getRotation());
            entity.put("hp", minion.getHp());
            entity.put("maxHp", minion.getMaxHp());
            entity.put("moveSpeed", minion.getMoveSpeed());
            entity.put("isDead", Boolean.TRUE.equals(minion.getDead()));
            entity.put("animationState", minion.getAnimationState());
            entity.put("targetEntityId", minion.getTargetEntityId());
            entity.put("targetType", minion.getTargetType());
            entity.put("modelUrl", minion.getModelUrl());
            entityList.add(entity);
        }
        return entityList;
    }

    private Map<String, List<Map<String, Object>>> buildStructureSnapshotEntities(BattleRoom room) {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<String, List<Map<String, Object>>>();
        result.put("towers", new ArrayList<Map<String, Object>>());
        result.put("nexuses", new ArrayList<Map<String, Object>>());
        result.put("inhibitors", new ArrayList<Map<String, Object>>());
        if (room == null || room.getStructures() == null) {
            return result;
        }
        for (BattleStructureState structure : room.getStructures()) {
            if (structure == null) {
                continue;
            }
            Map<String, Object> entity = new LinkedHashMap<String, Object>();
            entity.put("id", structure.getId());
            entity.put("team", structure.getTeam());
            entity.put("position", structure.getPosition());
            entity.put("hp", structure.getHp());
            entity.put("maxHp", structure.getMaxHp());
            entity.put("isDestroyed", structure.getIsDestroyed());
            entity.put("attackRange", structure.getAttackRange());
            entity.put("targetEntityId", structure.getTargetEntityId());
            if ("tower".equals(structure.getType())) {
                entity.put("type", structure.getSubType());
                result.get("towers").add(entity);
            } else if ("nexus".equals(structure.getType())) {
                result.get("nexuses").add(entity);
            } else if ("inhibitor".equals(structure.getType())) {
                result.get("inhibitors").add(entity);
            }
        }
        return result;
    }

    private List<Map<String, Object>> buildPlayerSnapshotEntities(BattleRoom room) {
        List<Map<String, Object>> playerList = new ArrayList<Map<String, Object>>();
        if (room == null || room.getPlayers() == null) {
            return playerList;
        }
        Map<String, String> championTeamMap = new HashMap<String, String>();
        for (BattleChampionState champion : room.getChampions()) {
            championTeamMap.put(champion.getId(), champion.getTeam());
        }
        for (PlayerSession ps : room.getPlayers()) {
            Map<String, Object> pMap = new LinkedHashMap<String, Object>();
            pMap.put("socketId", ps.getSessionId());
            pMap.put("playerName", ps.getPlayerName());
            pMap.put("championId", ps.getChampionId());
            pMap.put("team", championTeamMap.getOrDefault(ps.getChampionId(), ""));
            pMap.put("isSpectator", Boolean.TRUE.equals(ps.getSpectator()));
            playerList.add(pMap);
        }
        return playerList;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // ==================== 草丛视野 ====================

    /**
     * 从地图配置加载草丛 AABB 碰撞体。
     * 每个碰撞体表示为 [centerX, centerZ, halfWidth, halfDepth]。
     */
    private double[][] loadBushColliders() {
        JsonNode root = battleRoomManager.getLatestMapConfigRoot();
        if (root == null || root.isMissingNode()) return new double[0][];
        JsonNode colliders = root.path("bushes").path("colliders");
        if (!colliders.isArray()) return new double[0][];
        List<double[]> result = new ArrayList<double[]>();
        for (JsonNode node : colliders) {
            JsonNode center = node.path("center");
            JsonNode size = node.path("size");
            if (!center.isArray() || center.size() < 2 || !size.isArray() || size.size() < 2) continue;
            result.add(new double[]{
                    center.get(0).asDouble(),
                    center.get(1).asDouble(),
                    size.get(0).asDouble() / 2D,
                    size.get(1).asDouble() / 2D,
            });
        }
        return result.toArray(new double[0][]);
    }

    /**
     * 判定 (x, z) 所在的草丛索引，不在任何草丛返回 -1。
     */
    private int getBushIndex(double x, double z) {
        if (bushColliders == null) return -1;
        for (int i = 0; i < bushColliders.length; i++) {
            double[] b = bushColliders[i];
            if (Math.abs(x - b[0]) <= b[2] && Math.abs(z - b[1]) <= b[3]) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 按玩家视角过滤英雄快照列表。
     * 规则：敌方英雄在草丛中时不可见，除非己方有存活英雄在同一草丛。
     */
    private List<Map<String, Object>> filterChampionsByVision(
            List<Map<String, Object>> allChampions,
            BattleRoom room,
            String viewerTeam) {
        if (bushColliders == null || bushColliders.length == 0) return allChampions;

        Set<Integer> alliedBushIndices = new HashSet<Integer>();
        for (BattleChampionState c : room.getChampions()) {
            if (c == null || Boolean.TRUE.equals(c.getDead()) || c.getPosition() == null) continue;
            if (!viewerTeam.equals(c.getTeam())) continue;
            int bi = getBushIndex(c.getPosition().getX(), c.getPosition().getZ());
            if (bi >= 0) alliedBushIndices.add(bi);
        }

        List<Map<String, Object>> filtered = new ArrayList<Map<String, Object>>(allChampions.size());
        for (Map<String, Object> snap : allChampions) {
            String team = (String) snap.get("team");
            if (viewerTeam.equals(team)) {
                filtered.add(snap);
                continue;
            }
            Object posObj = snap.get("position");
            if (posObj instanceof BattleVector3) {
                BattleVector3 pos = (BattleVector3) posObj;
                int bi = getBushIndex(pos.getX(), pos.getZ());
                if (bi >= 0 && !alliedBushIndices.contains(bi)) {
                    continue;
                }
            }
            filtered.add(snap);
        }
        return filtered;
    }

    // ==================== 草丛视野上下文缓存 ====================

    private Set<String> resolveTeamBushIds(BattleRoom room, String team, TickComputationContext context) {
        if (team == null || context == null) return Collections.emptySet();
        Set<String> cached = context.teamBushIds.get(team);
        if (cached != null) return cached;
        Set<String> bushIds = new HashSet<String>();
        for (BattleChampionState c : room.getChampions()) {
            if (c == null || c.getPosition() == null || Boolean.TRUE.equals(c.getDead())) continue;
            if (!team.equals(c.getTeam())) continue;
            int bi = getBushIndex(c.getPosition().getX(), c.getPosition().getZ());
            if (bi >= 0) bushIds.add("bush_" + bi);
        }
        context.teamBushIds.put(team, bushIds);
        return bushIds;
    }

    private String resolveChampionBushId(BattleChampionState champion, TickComputationContext context) {
        if (champion == null || champion.getPosition() == null || context == null) return null;
        String champId = champion.getId();
        if (context.championBushIds.containsKey(champId)) {
            return context.championBushIds.get(champId);
        }
        int bi = getBushIndex(champion.getPosition().getX(), champion.getPosition().getZ());
        String bushId = bi >= 0 ? "bush_" + bi : null;
        context.championBushIds.put(champId, bushId);
        return bushId;
    }

    // ==================== 初始化辅助 ====================

    private void initStructureColliders() {
        JsonNode root = battleRoomManager.getLatestMapConfigRoot();
        if (root == null || root.isMissingNode()) {
            structureColliders = new double[0][];
            structureColliderTeams = new String[0];
            return;
        }
        JsonNode structuresNode = root.path("structures");
        List<double[]> colliderList = new ArrayList<double[]>();
        List<String> teamList = new ArrayList<String>();

        for (String structType : new String[]{"towers", "inhibitors", "nexuses"}) {
            JsonNode arr = structuresNode.path(structType);
            if (!arr.isArray()) continue;
            for (JsonNode node : arr) {
                JsonNode posArr = node.path("position");
                if (!posArr.isArray() || posArr.size() < 3) continue;
                double x = posArr.get(0).asDouble();
                double z = posArr.get(2).asDouble();
                double radius = node.path("collisionRadius").asDouble(3D);
                String team = node.path("team").asText("blue");
                colliderList.add(new double[]{x, z, radius});
                teamList.add(team);
            }
        }
        structureColliders = colliderList.toArray(new double[0][]);
        structureColliderTeams = teamList.toArray(new String[0]);
        log.info("已加载 {} 个建筑碰撞体", structureColliders.length);
    }

    private void initMinionConfig() {
        JsonNode minionRoot = resolveMinionConfigRoot();
        minionSpawnIntervalMs = minionRoot.path("spawnIntervalMs").asLong(25000L);
        log.info("小兵出兵间隔={}ms，出兵开关={}", minionSpawnIntervalMs,
                fishBattleServerProperties.getSpawnMinionsEnabled());
    }
}
