package com.cong.fishisland.socketio.battle;

import com.cong.fishisland.model.fishbattle.battle.*;
import com.cong.fishisland.config.fishbattle.FishBattleServerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 战斗模拟引擎（精简版 — 仅移动同步）。
 * 以固定间隔推进游戏状态（Tick），并定期广播战斗快照。
 * 后续迁移技能/战斗/小兵/建筑等模块时再扩展。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BattleSimulationService {
    private final Battle3dRoomManager battleRoomManager;
    private final Battle3dBroadcastService battleBroadcastService;
    private final InputQueue inputQueue;

    /** 地图可走区域边界（从 Battle3dRoomManager 读取，数据源为 DB config_key=map_default） */
    private double mapXMin;
    private double mapXMax;
    private double mapZMin;
    private double mapZMax;

    private static final int MAX_CATCHUP_TICKS = 4;

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
        // 从 Battle3dRoomManager 读取地图边界（数据来源为 DB fish_battle_config 表）
        mapXMin = battleRoomManager.getMapXMin();
        mapXMax = battleRoomManager.getMapXMax();
        mapZMin = battleRoomManager.getMapZMin();
        mapZMax = battleRoomManager.getMapZMax();

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

        drainAndApplyInputs(room, now);
        tickMovement(room, deltaSeconds, now);
        room.setGameTimer(room.getGameTimer() + deltaSeconds);

        if (snapshotEveryNTicks <= 1 || currentTick % snapshotEveryNTicks == 0) {
            emitSnapshot(room, now);
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
            double speed = champion.getMoveSpeed() != null ? champion.getMoveSpeed() : 3D;
            double step = Math.min(dist, speed * deltaSeconds);
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

    // ==================== 快照广播 ====================

    private void emitSnapshot(BattleRoom room, long now) {
        Map<String, Object> snapshotBase = new LinkedHashMap<String, Object>();
        snapshotBase.put("eventId", "snapshot-" + room.getSequence().incrementAndGet());
        snapshotBase.put("sequence", room.getSequence().get());
        snapshotBase.put("roomId", room.getRoomId());
        snapshotBase.put("serverTime", now);
        snapshotBase.put("serverTick", room.getTickNumber());
        snapshotBase.put("gameTimer", room.getGameTimer());

        List<Map<String, Object>> championList = buildChampionSnapshotEntities(room);
        List<Map<String, Object>> playerList = buildPlayerSnapshotEntities(room);

        if (room.getPlayers() == null || room.getPlayers().isEmpty()) {
            return;
        }
        for (PlayerSession playerSession : room.getPlayers()) {
            Map<String, Object> snapshot = new LinkedHashMap<String, Object>(snapshotBase);
            snapshot.put("champions", championList);
            snapshot.put("players", playerList);
            battleBroadcastService.sendToPlayer(playerSession, "combatSnapshot", snapshot);
        }
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
}
