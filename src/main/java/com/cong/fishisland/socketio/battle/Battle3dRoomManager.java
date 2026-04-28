package com.cong.fishisland.socketio.battle;

import com.cong.fishisland.model.fishbattle.battle.BattleChampionState;
import com.cong.fishisland.model.fishbattle.battle.BattleHealthRelicState;
import com.cong.fishisland.model.fishbattle.battle.BattleStructureState;
import com.cong.fishisland.model.fishbattle.battle.BattleRoom;
import com.cong.fishisland.model.fishbattle.battle.BattleVector3;
import com.cong.fishisland.model.fishbattle.battle.PlayerSession;
import com.cong.fishisland.model.entity.fishbattle.FishBattleHero;
import com.cong.fishisland.service.fishbattle.FishBattleConfigService;
import com.cong.fishisland.service.fishbattle.FishBattleHeroService;
import com.corundumstudio.socketio.SocketIOClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 3D战斗房间管理器（精简版 — 仅支持移动同步）。
 * 后续迁移建筑、小兵、技能等模块时再扩展。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Battle3dRoomManager {
    private final FishBattleHeroService fishBattleHeroService;
    private final FishBattleConfigService fishBattleConfigService;
    private final ObjectMapper objectMapper;
    private final HeroSkillDefinitionService heroSkillDefinitionService;

    /** heroId → FishBattleHero 缓存 */
    private final Map<String, FishBattleHero> heroAttrCache = new ConcurrentHashMap<>();

    /** roomCode → BattleRoom 映射（支持多房间） */
    private final Map<String, BattleRoom> battleRooms = new ConcurrentHashMap<>();

    /** roomCode → userId→championId 映射 */
    private final Map<String, Map<Long, String>> userChampionMapping = new ConcurrentHashMap<>();

    /** roomCode → 已发送 battle:sceneReady 的 userId 集合 */
    private final Map<String, Set<Long>> sceneReadyUsers = new ConcurrentHashMap<>();

    /** roomCode → 房间内总玩家数 */
    private final Map<String, Integer> roomPlayerCounts = new ConcurrentHashMap<>();

    /** 出生点（从 DB config_key=map_default 读取，Single Source of Truth） */
    private List<double[]> blueSpawns;
    private List<double[]> redSpawns;

    /** 地图可走区域边界（从 DB 读取，供 BattleSimulationService 使用） */
    private double mapXMin = -130;
    private double mapXMax = 130;
    private double mapZMin = -19.6;
    private double mapZMax = 19.6;

    private JsonNode latestMapConfigRoot;

    @PostConstruct
    public void init() {
        // 预加载英雄属性缓存
        try {
            List<FishBattleHero> heroes = fishBattleHeroService.listEnabledHeroes();
            for (FishBattleHero hero : heroes) {
                heroAttrCache.put(hero.getHeroId(), hero);
            }
            log.info("英雄属性缓存预加载完成，共 {} 个英雄", heroAttrCache.size());
        } catch (Exception e) {
            log.warn("英雄属性缓存预加载失败，将使用默认值: {}", e.getMessage());
        }

        // 初始加载地图配置（启动时读一次防止空指针，每局开始时会重新读取最新 DB 配置）
        loadMapConfig();
    }

    /**
     * 从 fish_battle_config 表 config_key=map_default 读取出生点和地图边界。
     * 失败时使用默认值。
     */
    private void loadMapConfig() {
        // 默认出生点
        blueSpawns = Arrays.asList(
                new double[]{-125, 0, -5}, new double[]{-120, 0, -2}, new double[]{-125, 0, 0},
                new double[]{-120, 0, 2}, new double[]{-125, 0, 5}
        );
        redSpawns = Arrays.asList(
                new double[]{125, 0, -5}, new double[]{120, 0, -2}, new double[]{125, 0, 0},
                new double[]{120, 0, 2}, new double[]{125, 0, 5}
        );
        try {
            String configJson = fishBattleConfigService.getConfigData("map_default");
            if (configJson == null || configJson.isEmpty()) {
                log.warn("地图配置 map_default 未找到，使用默认出生点和边界");
                return;
            }
            JsonNode root = objectMapper.readTree(configJson);
            latestMapConfigRoot = root;

            // 解析出生点 spawnLayouts
            JsonNode spawnNode = root.path("spawnLayouts");
            if (!spawnNode.isMissingNode()) {
                List<double[]> parsedBlue = parseSpawnArray(spawnNode.path("blue"));
                List<double[]> parsedRed = parseSpawnArray(spawnNode.path("red"));
                if (!parsedBlue.isEmpty()) blueSpawns = parsedBlue;
                if (!parsedRed.isEmpty()) redSpawns = parsedRed;
            }

            // 解析可走区域边界 playableBounds
            JsonNode boundsNode = root.path("map").path("playableBounds");
            if (!boundsNode.isMissingNode()) {
                mapXMin = boundsNode.path("minX").asDouble(mapXMin);
                mapXMax = boundsNode.path("maxX").asDouble(mapXMax);
                mapZMin = boundsNode.path("minZ").asDouble(mapZMin);
                mapZMax = boundsNode.path("maxZ").asDouble(mapZMax);
            }

            log.info("地图配置加载完成: blueSpawns={}, redSpawns={}, bounds=[{},{},{},{}]",
                    blueSpawns.size(), redSpawns.size(), mapXMin, mapXMax, mapZMin, mapZMax);
        } catch (Exception e) {
            latestMapConfigRoot = null;
            log.warn("地图配置解析失败，使用默认值: {}", e.getMessage());
        }
    }

    private List<double[]> parseSpawnArray(JsonNode arrayNode) {
        List<double[]> result = new ArrayList<>();
        if (arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                if (item.isArray() && item.size() >= 3) {
                    result.add(new double[]{item.get(0).asDouble(), item.get(1).asDouble(), item.get(2).asDouble()});
                }
            }
        }
        return result;
    }

    /** 获取地图边界（供 BattleSimulationService 使用） */
    public double getMapXMin() { return mapXMin; }
    public double getMapXMax() { return mapXMax; }
    public double getMapZMin() { return mapZMin; }
    public double getMapZMax() { return mapZMax; }

    /**
     * 刷新英雄属性缓存（从 DB 重新读取）。
     * 每局开始时调用，确保修改数据库后下一局即时生效。
     */
    private void refreshHeroAttrCache() {
        try {
            List<FishBattleHero> heroes = fishBattleHeroService.listEnabledHeroes();
            heroAttrCache.clear();
            for (FishBattleHero hero : heroes) {
                heroAttrCache.put(hero.getHeroId(), hero);
            }
            log.info("英雄属性缓存已刷新，共 {} 个英雄", heroAttrCache.size());
        } catch (Exception e) {
            log.warn("英雄属性缓存刷新失败，沿用旧缓存: {}", e.getMessage());
        }
    }

    private FishBattleHero resolveHeroAttributes(String heroId) {
        return heroAttrCache.computeIfAbsent(heroId, id -> {
            FishBattleHero hero = fishBattleHeroService.getByHeroId(id);
            if (hero == null) {
                log.warn("未找到英雄属性 heroId={}, 使用默认值", id);
            }
            return hero;
        });
    }

    public BattleRoom getRoom(String roomCode) {
        if (roomCode == null || roomCode.trim().isEmpty()) {
            return null;
        }
        return battleRooms.get(roomCode);
    }

    public Collection<BattleRoom> getActiveRooms() {
        return new ArrayList<BattleRoom>(battleRooms.values());
    }

    /**
     * 基于真实选英雄数据创建战斗房间。
     * 每次创建时重新读取 DB 地图配置，确保修改数据库后下一局即时生效。
     */
    public BattleRoom createRoomFromPlayers(String roomCode,
                                            Collection<com.cong.fishisland.socketio.FishBattleRoomManager.PlayerConnection> players) {
        // 每局开始时重新从 DB 读取最新配置，确保修改数据库后下一局即时生效
        loadMapConfig();
        refreshHeroAttrCache();
        BattleRoom battleRoom = BattleRoom.empty(roomCode);
        Map<Long, String> userMapping = new LinkedHashMap<>();

        int blueIndex = 0;
        int redIndex = 0;

        for (com.cong.fishisland.socketio.FishBattleRoomManager.PlayerConnection p : players) {
            String heroId = p.getSelectedHeroId();
            if (heroId == null || heroId.trim().isEmpty()) {
                continue;
            }
            String team = p.getTeam();
            int teamIndex = "blue".equals(team) ? blueIndex++ : redIndex++;
            String championId = team + "_" + teamIndex;

            double[] spawn = resolveSpawnPosition(team, teamIndex);

            FishBattleHero heroAttr = resolveHeroAttributes(heroId);
            double heroHp = heroAttr != null && heroAttr.getBaseHp() != null ? heroAttr.getBaseHp().doubleValue() : 1000D;
            double heroMp = heroAttr != null && heroAttr.getBaseMp() != null ? heroAttr.getBaseMp().doubleValue() : 600D;
            double heroAd = heroAttr != null && heroAttr.getBaseAd() != null ? heroAttr.getBaseAd().doubleValue() : 60D;
            double heroMoveSpeed = heroAttr != null && heroAttr.getMoveSpeed() != null ? heroAttr.getMoveSpeed().doubleValue() / 100.0 : 3D;

            String effectiveModelUrl = p.getSkinModelUrl() != null ? p.getSkinModelUrl() : p.getHeroModelUrl();

            battleRoom.getChampions().add(BattleChampionState.builder()
                    .id(championId)
                    .heroId(heroId)
                    .skin(p.getSkinModelUrl())
                    .modelUrl(effectiveModelUrl)
                    .playerName(p.getPlayerName())
                    .team(team)
                    .position(BattleVector3.builder().x(spawn[0]).y(spawn[1]).z(spawn[2]).build())
                    .rotation("blue".equals(team) ? 0D : Math.PI)
                    .moveSpeed(heroMoveSpeed)
                    .baseMoveSpeed(heroMoveSpeed)
                    .moveTarget(null)
                    .animationState("idle")
                    .inputMode("idle")
                    .dead(Boolean.FALSE)
                    .isDead(Boolean.FALSE)
                    .hp(heroHp)
                    .maxHp(heroHp)
                    .mp(heroMp)
                    .maxMp(heroMp)
                    .shield(0D)
                    .flowValue(0D)
                    .baseAd(heroAd)
                    .skillStates(createInitialSkillStates(p.getSpell1(), p.getSpell2()))
                    .activeCastInstanceId(null)
                    .activeCastPhase("idle")
                    .movementLockedUntil(0L)
                    .idleStartedAt(System.currentTimeMillis())
                    .build());

            userMapping.put(p.getUserId(), championId);
        }

        battleRoom.getStructures().addAll(buildInitialStructures());
        battleRoom.getHealthRelics().addAll(buildInitialHealthRelics());
        long now = System.currentTimeMillis();
        long spawnIntervalMs = latestMapConfigRoot != null
                ? latestMapConfigRoot.path("minions").path("spawnIntervalMs").asLong(25000L)
                : 25000L;
        battleRoom.setNextMinionSpawnAt(now + Math.max(1000L, spawnIntervalMs));
        battleRoom.setMinionWaveSequence(0L);

        battleRooms.put(roomCode, battleRoom);
        userChampionMapping.put(roomCode, userMapping);
        sceneReadyUsers.put(roomCode, new CopyOnWriteArraySet<>());
        roomPlayerCounts.put(roomCode, userMapping.size());

        log.info("战斗房间已创建: roomCode={}, 英雄数量={}, 补血道具数量={}",
                roomCode, battleRoom.getChampions().size(), battleRoom.getHealthRelics().size());
        return battleRoom;
    }

    /**
     * 通过 userId 加入真实战斗房间（已有预分配英雄）。
     */
    public PlayerSession joinByUserId(String roomCode, Long userId, String playerName, SocketIOClient client) {
        Map<Long, String> mapping = userChampionMapping.get(roomCode);
        BattleRoom battleRoom = battleRooms.get(roomCode);
        if (mapping == null || battleRoom == null) {
            return null;
        }

        String championId = mapping.get(userId);
        String sessionId = client.getSessionId().toString();

        // 防串房：先从所有房间清除该 sessionId / userId 的残留记录
        for (BattleRoom otherRoom : battleRooms.values()) {
            if (otherRoom == battleRoom) {
                continue;
            }
            boolean removed = otherRoom.getPlayers().removeIf(
                    item -> sessionId.equals(item.getSessionId()) || userId.equals(item.getUserId()));
            if (removed) {
                log.warn("防串房清理: 从房间 {} 中移除了 userId={}/sessionId={} 的残留会话",
                        otherRoom.getRoomId(), userId, sessionId);
            }
        }

        battleRoom.getPlayers().removeIf(item -> userId.equals(item.getUserId()));

        if (championId != null) {
            battleRoom.getPlayers().stream()
                    .filter(item -> championId.equals(item.getChampionId()))
                    .findFirst()
                    .ifPresent(oldSession -> {
                        SocketIOClient oldClient = oldSession.getClient();
                        if (oldClient != null && oldClient.isChannelOpen()
                                && !oldClient.getSessionId().equals(client.getSessionId())) {
                            Map<String, Object> payload = new LinkedHashMap<>();
                            payload.put("reason", "该账号已在其他地方进入战斗");
                            payload.put("serverTime", System.currentTimeMillis());
                            oldClient.sendEvent("session:superseded", payload);
                            oldClient.disconnect();
                            log.info("踢出旧战斗会话: roomCode={}, oldSessionId={}, newSessionId={}",
                                    roomCode, oldSession.getSessionId(), sessionId);
                        }
                    });
            battleRoom.getPlayers().removeIf(item -> championId.equals(item.getChampionId()));
        }
        battleRoom.getPlayers().removeIf(item -> item.getSessionId().equals(sessionId));

        PlayerSession playerSession = PlayerSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .playerName(playerName != null ? playerName : "玩家")
                .championId(championId)
                .spectator(championId == null)
                .client(client)
                .build();

        battleRoom.getPlayers().add(playerSession);
        log.info("玩家加入战斗房间: roomCode={}, userId={}, championId={}, sessionId={}", roomCode, userId, championId, sessionId);
        return playerSession;
    }

    public boolean markSceneReady(String roomCode, Long userId) {
        Set<Long> readySet = sceneReadyUsers.get(roomCode);
        Integer totalPlayers = roomPlayerCounts.get(roomCode);
        if (readySet == null || totalPlayers == null) {
            return false;
        }
        readySet.add(userId);
        boolean allReady = readySet.size() >= totalPlayers;
        if (allReady) {
            log.info("全员 3D 场景加载就绪: roomCode={}, totalPlayers={}", roomCode, totalPlayers);
        }
        return allReady;
    }

    public boolean hasBattleRoom(String roomCode) {
        return roomCode != null && battleRooms.containsKey(roomCode);
    }

    public void removeSessionById(String sessionId) {
        if (sessionId == null) {
            return;
        }
        battleRooms.values().forEach(battleRoom ->
                battleRoom.getPlayers().removeIf(item -> item.getSessionId().equals(sessionId)));
    }

    public Optional<BattleChampionState> findChampion(BattleRoom battleRoom, String championId) {
        if (battleRoom == null || championId == null || championId.trim().isEmpty()) {
            return Optional.empty();
        }
        return battleRoom.getChampions().stream()
                .filter(item -> item.getId().equals(championId))
                .findFirst();
    }

    public BattleRoom findRoomByChampionId(String championId) {
        if (championId == null || championId.trim().isEmpty()) {
            return null;
        }
        return battleRooms.values().stream()
                .filter(battleRoom -> battleRoom.getChampions() != null
                        && battleRoom.getChampions().stream().anyMatch(item -> championId.equals(item.getId())))
                .findFirst()
                .orElse(null);
    }

    public BattleRoom findRoomBySessionId(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        BattleRoom found = null;
        for (BattleRoom battleRoom : battleRooms.values()) {
            boolean match = battleRoom.getPlayers().stream()
                    .anyMatch(item -> sessionId.equals(item.getSessionId()));
            if (match) {
                if (found != null) {
                    log.error("串房检测: sessionId={} 同时出现在房间 {} 和 {}，强制从后者移除",
                            sessionId, found.getRoomId(), battleRoom.getRoomId());
                    battleRoom.getPlayers().removeIf(item -> sessionId.equals(item.getSessionId()));
                    continue;
                }
                found = battleRoom;
            }
        }
        return found;
    }

    public PlayerSession findPlayerSessionBySessionId(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        return battleRooms.values().stream()
                .flatMap(battleRoom -> battleRoom.getPlayers().stream())
                .filter(item -> sessionId.equals(item.getSessionId()))
                .findFirst()
                .orElse(null);
    }

    public void destroyRoom(String roomCode) {
        if (roomCode == null) {
            return;
        }
        battleRooms.remove(roomCode);
        userChampionMapping.remove(roomCode);
        sceneReadyUsers.remove(roomCode);
        roomPlayerCounts.remove(roomCode);
        log.info("战斗房间已销毁: roomCode={}", roomCode);
    }

    public Map<Long, String> getUserChampionMapping(String roomCode) {
        Map<Long, String> mapping = userChampionMapping.get(roomCode);
        return mapping != null ? new LinkedHashMap<>(mapping) : Collections.emptyMap();
    }

    public JsonNode getLatestMapConfigRoot() {
        return latestMapConfigRoot;
    }

    private double[] resolveSpawnPosition(String team, int teamIndex) {
        List<double[]> layouts = "blue".equals(team) ? blueSpawns : redSpawns;
        if (layouts == null || layouts.isEmpty()) {
            return new double[]{0D, 0D, 0D};
        }
        if (teamIndex >= 0 && teamIndex < layouts.size()) {
            return layouts.get(teamIndex);
        }
        return layouts.get(0);
    }

    private List<BattleStructureState> buildInitialStructures() {
        List<BattleStructureState> structures = new ArrayList<BattleStructureState>();
        JsonNode root = latestMapConfigRoot;
        if (root == null || root.isMissingNode()) {
            return structures;
        }
        JsonNode structureRoot = root.path("structures");
        appendStructures(structures, structureRoot.path("towers"), "tower");
        appendStructures(structures, structureRoot.path("nexuses"), "nexus");
        appendStructures(structures, structureRoot.path("inhibitors"), "inhibitor");
        return structures;
    }

    private List<BattleHealthRelicState> buildInitialHealthRelics() {
        List<BattleHealthRelicState> relics = new ArrayList<BattleHealthRelicState>();
        JsonNode root = latestMapConfigRoot;
        if (root == null || root.isMissingNode()) {
            return relics;
        }
        JsonNode relicArray = root.path("healthRelics").path("items");
        if (relicArray == null || !relicArray.isArray()) {
            return relics;
        }
        for (JsonNode node : relicArray) {
            if (node == null || !node.isObject()) {
                continue;
            }
            BattleVector3 position = parseVector3(node.path("position"));
            if (position == null) {
                continue;
            }
            String id = node.path("id").asText();
            if (id == null || id.trim().isEmpty()) {
                continue;
            }
            relics.add(BattleHealthRelicState.builder()
                    .id(id)
                    .position(position)
                    .isAvailable(Boolean.TRUE)
                    .respawnAt(null)
                    .healPercent(node.path("healPercent").asDouble(0.15D))
                    .pickupRadius(node.path("pickupRadius").asDouble(2.5D))
                    .build());
        }
        return relics;
    }

    private void appendStructures(List<BattleStructureState> structures, JsonNode arrayNode, String structureType) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return;
        }
        for (JsonNode node : arrayNode) {
            if (node == null || !node.isObject()) {
                continue;
            }
            BattleVector3 position = parseVector3(node.path("position"));
            if (position == null) {
                continue;
            }
            String id = node.path("id").asText();
            if (id == null || id.trim().isEmpty()) {
                continue;
            }
            double maxHp = node.path("maxHp").asDouble(0D);
            boolean isTower = "tower".equals(structureType);
            structures.add(BattleStructureState.builder()
                    .id(id)
                    .type(structureType)
                    .subType(isTower ? node.path("subType").asText(node.path("type").asText(null)) : null)
                    .team(node.path("team").asText("blue"))
                    .position(position)
                    .collisionRadius(node.path("collisionRadius").asDouble(0D))
                    .hp(maxHp)
                    .maxHp(maxHp)
                    .isDestroyed(Boolean.FALSE)
                    .armor(node.path("armor").asDouble(0D))
                    .attackDamage(node.path("attackDamage").asDouble(isTower ? 150D : 0D))
                    .attackRange(node.path("attackRange").asDouble(isTower ? 20D : 0D))
                    .attackSpeed(node.path("attackSpeed").asDouble(isTower ? 1D : 0D))
                    .lastAttackAt(0L)
                    .targetEntityId(null)
                    .build());
        }
    }

    private BattleVector3 parseVector3(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray() || arrayNode.size() < 3) {
            return null;
        }
        return BattleVector3.builder()
                .x(arrayNode.get(0).asDouble())
                .y(arrayNode.get(1).asDouble())
                .z(arrayNode.get(2).asDouble())
                .build();
    }

    private Map<String, Map<String, Object>> createInitialSkillStates(String spell1, String spell2) {
        Map<String, Map<String, Object>> skillStates = new LinkedHashMap<String, Map<String, Object>>();
        appendSingleSkillState(skillStates, heroSkillDefinitionService.findSkillBySlot(null, "basicAttack"));
        appendSingleSkillState(skillStates, heroSkillDefinitionService.getSummonerSpellDefinition(
                spell1 != null && !spell1.trim().isEmpty() ? spell1 : "flash",
                "summonerD"
        ));
        appendSingleSkillState(skillStates, heroSkillDefinitionService.getSummonerSpellDefinition(
                spell2 != null && !spell2.trim().isEmpty() ? spell2 : "heal",
                "summonerF"
        ));
        return skillStates;
    }

    private void appendSingleSkillState(Map<String, Map<String, Object>> skillStates, JsonNode skill) {
        if (skill == null || skill.isMissingNode() || !skill.isObject()) {
            return;
        }
        String slot = skill.path("slot").asText(null);
        if (slot == null || slot.trim().isEmpty()) {
            return;
        }
        Map<String, Object> state = new LinkedHashMap<String, Object>();
        state.put("slot", slot);
        state.put("skillId", skill.path("skillId").asText(slot));
        state.put("name", skill.path("name").asText(slot));
        state.put("level", skill.path("initialLevel").asInt(1));
        state.put("maxCooldownMs", skill.path("cooldown").path("baseMs").asLong(0L));
        state.put("remainingCooldownMs", 0L);
        state.put("isReady", Boolean.TRUE);
        state.put("insufficientResource", Boolean.FALSE);
        state.put("isSecondPhase", Boolean.FALSE);
        state.put("isCasting", Boolean.FALSE);
        skillStates.put(slot, state);
    }

}
