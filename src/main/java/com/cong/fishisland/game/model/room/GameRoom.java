package com.cong.fishisland.game.model.room;

import com.alibaba.fastjson2.annotation.JSONField;
import com.cong.fishisland.game.enums.GameTypeEnum;
import com.cong.fishisland.game.enums.PlayerRoleEnum;
import com.cong.fishisland.game.enums.RoomStateEnum;
import com.cong.fishisland.game.landlords.model.poker.PokerHand;
import com.cong.fishisland.game.model.dto.response.PlayerInfoResp;
import com.cong.fishisland.game.model.dto.response.RoomInfoResp;
import com.cong.fishisland.game.model.player.GamePlayer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 游戏房间
 *
 * @author cong
 */
@Data
@Slf4j
public class GameRoom {

    /**
     * 房间ID
     */
    private String roomId;

    /**
     * 游戏类型
     */
    private GameTypeEnum gameType;

    /**
     * 房间状态
     */
    private RoomStateEnum state;

    /**
     * 房主ID
     */
    private Long ownerId;

    /**
     * 最大玩家数
     */
    private int maxPlayers;

    /**
     * 当前玩家数
     */
    private int currentPlayers;

    /**
     * 房间密码
     */
    private String password;

    /**
     * 是否需要密码
     */
    private boolean needPassword;

    /**
     * 玩家列表
     * -- SETTER --
     * 设置玩家 Map
     */
    private Map<Long, GamePlayer> players;

    /**
     * 获取玩家顺序 (用于确定出牌顺序)
     */
    private List<Long> playerOrder;

    /**
     * 底牌
     */
    private PokerHand bottomCards;

    /**
     * 地主ID
     */
    private Long landlordId;

    /**
     * 当前出牌玩家ID
     */
    private Long currentPlayerId;

    /**
     * 上一个出牌玩家ID
     */
    private Long lastPlayerId;

    /**
     * 上一个出牌 (用于验证)
     */
    private PokerHand lastPlayedCards;

    /**
     * 当前叫分玩家ID
     */
    private Long currentRobPlayerId;

    /**
     * 当前最高叫分
     */
    private int highestRobScore;

    /**
     * 已叫分的玩家
     */
    private Set<Long> robbedPlayers;

    /**
     * 不叫的玩家
     */
    private Set<Long> passedRobPlayers;

    /**
     * 叫地主轮次开始的玩家ID（用于判断一轮结束）
     */
    private Long robRoundStartPlayerId;

    /**
     * 最后一个叫分的玩家ID（用于确定地主）
     */
    private Long lastRobPlayerId;

    /**
     * 是否是癞子模式
     */
    private boolean laiZiMode;

    /**
     * 癞子牌面值
     */
    private int universalValue;

    /**
     * 是否是技能模式
     */
    private boolean skillMode;

    /**
     * 是否不洗牌模式
     */
    private boolean dontShuffleMode;

    /**
     * 是否允许聊天
     */
    private boolean enableChat;

    /**
     * 是否显示IP
     */
    private boolean showIP;

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 最后活跃时间
     */
    private long lastActiveTime;

    /**
     * 游戏开始时间
     */
    private long gameStartTime;

    /**
     * 获取玩家顺序列表
     */
    @JSONField(serialize = false)
    public List<Long> getPlayerOrder() {
        if (playerOrder == null) {
            playerOrder = new ArrayList<>();
        }
        return playerOrder;
    }

    public GameRoom() {
        this.players = new ConcurrentHashMap<>();
        this.playerOrder = new ArrayList<>();
        this.state = RoomStateEnum.WAITING;
        this.bottomCards = new PokerHand();
        this.robbedPlayers = new HashSet<>();
        this.passedRobPlayers = new HashSet<>();
        this.createTime = System.currentTimeMillis();
        this.lastActiveTime = this.createTime;
    }

    public GameRoom(String roomId, GameTypeEnum gameType, Long ownerId) {
        this();
        this.roomId = roomId;
        this.gameType = gameType;
        this.ownerId = ownerId;
        this.maxPlayers = gameType.getPlayerCount();
    }

    /**
     * 添加玩家
     */
    public boolean addPlayer(GamePlayer player) {
        if (players == null) {
            players = new ConcurrentHashMap<>();
        }
        if (players.size() >= maxPlayers) {
            return false;
        }
        if (players.containsKey(player.getUserId())) {
            return false;
        }

        PlayerRoleEnum role = players.isEmpty() ? PlayerRoleEnum.OWNER : PlayerRoleEnum.PLAYER;
        player.setRole(role);
        players.put(player.getUserId(), player);
        if (!playerOrder.contains(player.getUserId())) {
            playerOrder.add(player.getUserId());
        }
        currentPlayers = players.size();
        updateLastActiveTime();
        return true;
    }

    /**
     * 获取玩家 Map
     */
    public Map<Long, GamePlayer> getPlayers() {
        if (players == null) {
            players = new ConcurrentHashMap<>();
        }
        return players;
    }

    /**
     * 移除玩家
     */
    public boolean removePlayer(Long userId) {
        if (players == null) {
            return false;
        }
        GamePlayer player = players.remove(userId);
        if (player == null) {
            return false;
        }

        playerOrder.remove(userId);

        // 如果是房主，转移给下一个玩家
        if (ownerId.equals(userId) && !playerOrder.isEmpty()) {
            Long newOwnerId = playerOrder.get(0);
            ownerId = newOwnerId;
            GamePlayer newOwner = players.get(newOwnerId);
            if (newOwner != null) {
                newOwner.setRole(PlayerRoleEnum.OWNER);
            }
        }

        currentPlayers = players.size();
        updateLastActiveTime();
        return true;
    }

    /**
     * 获取玩家
     */
    public GamePlayer getPlayer(Long userId) {
        if (players == null) {
            return null;
        }
        return players.get(userId);
    }

    /**
     * 获取所有玩家
     */
    @JSONField(serialize = false)
    public List<GamePlayer> getAllPlayers() {
        if (players == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(players.values());
    }

    /**
     * 获取按顺序排列的玩家
     */
    @JSONField(serialize = false)
    public List<GamePlayer> getOrderedPlayers() {
        if (players == null) {
            return new ArrayList<>();
        }
        List<GamePlayer> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Long userId : playerOrder) {
            if (seen.contains(userId)) {
                continue;
            }
            GamePlayer player = players.get(userId);
            if (player != null) {
                result.add(player);
                seen.add(userId);
            }
        }
        return result;
    }

    /**
     * 获取玩家数量
     */
    public int getPlayerCount() {
        if (players == null) {
            return 0;
        }
        return players.size();
    }

    /**
     * 获取在线玩家数量
     */
    public int getOnlinePlayerCount() {
        if (players == null) {
            return 0;
        }
        return (int) players.values().stream()
                .filter(GamePlayer::isOnline)
                .count();
    }

    /**
     * 是否所有玩家都已准备
     */
    public boolean isAllReady() {
        if (players == null || players.size() < 3) {
            return false;
        }
        return players.values().stream()
                .allMatch(GamePlayer::isReady);
    }

    /**
     * 获取下一个玩家ID
     */
    public Long getNextPlayerId(Long currentId) {
        int index = playerOrder.indexOf(currentId);
        if (index < 0 || playerOrder.size() <= 1) {
            return null;
        }
        return playerOrder.get((index + 1) % playerOrder.size());
    }

    /**
     * 获取上一个玩家ID
     */
    public Long getPrevPlayerId(Long currentId) {
        int index = playerOrder.indexOf(currentId);
        if (index < 0 || playerOrder.size() <= 1) {
            return null;
        }
        return playerOrder.get((index - 1 + playerOrder.size()) % playerOrder.size());
    }

    /**
     * 设置地主
     */
    public void setLandlord(Long userId) {
        if (players == null) {
            return;
        }
        // 清除旧地主
        if (landlordId != null) {
            GamePlayer oldLandlord = players.get(landlordId);
            if (oldLandlord != null) {
                oldLandlord.removeLandlord();
            }
        }

        landlordId = userId;

        // 设置新地主
        GamePlayer newLandlord = players.get(userId);
        if (newLandlord != null) {
            newLandlord.setAsLandlord();
            // 添加底牌
            newLandlord.getHand().addAll(bottomCards.getAll());
        }
    }

    /**
     * 是否是地主
     */
    public boolean isLandlord(Long userId) {
        return landlordId != null && landlordId.equals(userId);
    }

    /**
     * 是否可以开始游戏
     */
    public boolean canStartGame() {
        return state == RoomStateEnum.WAITING || state == RoomStateEnum.READY;
    }

    /**
     * 设置密码
     */
    public void setPassword(String password) {
        this.password = password;
        this.needPassword = password != null && !password.isEmpty();
    }

    /**
     * 验证密码
     */
    public boolean verifyPassword(String inputPassword) {
        if (!needPassword) {
            return true;
        }
        return password != null && password.equals(inputPassword);
    }

    /**
     * 更新最后活跃时间
     */
    public void updateLastActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }

    /**
     * 获取房间简要信息
     */
    @JSONField(serialize = false)
    public RoomInfo getInfo() {
        List<GamePlayer.PlayerInfo> playerInfos = (players == null)
                ? new ArrayList<>()
                : players.values().stream()
                        .map(GamePlayer::getInfo)
                        .collect(Collectors.toList());
        return new RoomInfo(
                roomId,
                gameType,
                state,
                ownerId,
                maxPlayers,
                currentPlayers,
                needPassword,
                playerInfos
        );
    }

    /**
     * 房间信息 (用于传输)
     */
    @Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class RoomInfo {
        private String roomId;
        private GameTypeEnum gameType;
        private RoomStateEnum state;
        private Long ownerId;
        private int maxPlayers;
        private int currentPlayers;
        private boolean hasPassword;
        private List<GamePlayer.PlayerInfo> players;
    }

    /**
     * 转换为 RoomInfoResp
     */
    public RoomInfoResp toRoomInfoResp() {
        List<PlayerInfoResp> playerList = getOrderedPlayers().stream()
                .map(GamePlayer::toPlayerInfoResp)
                .collect(Collectors.toList());
        return RoomInfoResp.builder()
                .roomId(roomId)
                .gameType(gameType)
                .state(state)
                .ownerId(ownerId)
                .playerCount(getPlayerCount())
                .maxPlayers(maxPlayers)
                .needPassword(needPassword)
                .players(playerList)
                .build();
    }
}
