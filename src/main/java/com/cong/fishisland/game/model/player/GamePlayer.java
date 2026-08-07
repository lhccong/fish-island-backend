package com.cong.fishisland.game.model.player;

import com.alibaba.fastjson2.annotation.JSONField;
import com.cong.fishisland.game.enums.PlayerRoleEnum;
import com.cong.fishisland.game.enums.RobotReasonEnum;
import com.cong.fishisland.game.landlords.model.poker.Poker;
import com.cong.fishisland.game.landlords.model.poker.PokerHand;
import com.cong.fishisland.game.model.dto.response.PlayerInfoResp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 游戏玩家
 *
 * @author cong
 */
@Data
public class GamePlayer {
    
    /**
     * 玩家ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String userName;
    
    /**
     * 头像
     */
    private String avatar;
    
    /**
     * 角色 (房主/玩家/观战)
     */
    private PlayerRoleEnum role;
    
    /**
     * 手牌
     */
    private PokerHand hand;
    
    /**
     * 当前出牌 (显示在桌面上)
     */
    private List<Poker> currentPlayedCards;
    
    /**
     * 是否准备
     */
    private boolean ready;
    
    /**
     * 是否在线
     */
    private boolean online;
    
    /**
     * 是否已出完牌
     */
    private boolean finished;
    
    /**
     * 是否是地主
     */
    private boolean isLandlord;
    
    /**
     * 叫分 (0表示不叫)
     */
    private int robScore;
    
    /**
     * 最后出牌时间
     */
    private long lastPlayTime;
    
    /**
     * 是否被AI托管
     */
    private boolean robotControlled;
    
    /**
     * AI托管原因
     * @see com.cong.fishisland.game.enums.RobotReasonEnum
     */
    private RobotReasonEnum robotReason;

    public GamePlayer() {
        this.hand = new PokerHand();
        this.currentPlayedCards = new ArrayList<>();
        this.ready = false;
        this.online = true;
        this.finished = false;
        this.isLandlord = false;
        this.robScore = 0;
        this.robotControlled = false;
        this.robotReason = null;
    }

    public GamePlayer(Long userId, String userName, String avatar) {
        this();
        this.userId = userId;
        this.userName = userName;
        this.avatar = avatar;
    }

    /**
     * 获取手牌数量
     */
    @JSONField(serialize = false)
    public int getCardCount() {
        return hand != null ? hand.size() : 0;
    }

    /**
     * 获取手牌ID列表
     */
    @JSONField(serialize = false)
    public List<String> getCardIds() {
        return hand != null ? hand.toIdList() : new ArrayList<>();
    }

    /**
     * 获取当前出牌ID列表
     */
    @JSONField(serialize = false)
    public List<String> getCurrentPlayedCardIds() {
        if (currentPlayedCards == null) return new ArrayList<>();
        return currentPlayedCards.stream().map(Poker::getId).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 设置当前出牌
     */
    public void setCurrentPlayedCards(List<Poker> cards) {
        this.currentPlayedCards = cards != null ? new ArrayList<>(cards) : new ArrayList<>();
    }

    /**
     * 清除当前出牌
     */
    @JSONField(serialize = false)
    public void clearCurrentPlayedCards() {
        if (this.currentPlayedCards != null) {
            this.currentPlayedCards.clear();
        }
    }

    /**
     * 是否可以开始游戏
     */
    @JSONField(serialize = false)
    public boolean canStartGame() {
        return role == PlayerRoleEnum.OWNER && ready;
    }

    /**
     * 设置为地主
     */
    @JSONField(serialize = false)
    public void setAsLandlord() {
        this.isLandlord = true;
    }

    /**
     * 设置为非地主
     */
    @JSONField(serialize = false)
    public void removeLandlord() {
        this.isLandlord = false;
    }

    /**
     * 获取玩家简要信息
     */
    @JSONField(serialize = false)
    public PlayerInfo getInfo() {
        return new PlayerInfo(userId, userName, avatar, role, hand.size(), ready, online, isLandlord, robScore, robotControlled);
    }

    /**
     * 玩家信息 (用于传输)
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlayerInfo {
        private Long userId;
        private String userName;
        private String avatar;
        private PlayerRoleEnum role;
        private int cardCount;
        private boolean ready;
        private boolean online;
        private boolean isLandlord;
        private int robScore;
        private boolean robotControlled;
    }

    /**
     * 转换为 PlayerInfoResp
     */
    public PlayerInfoResp toPlayerInfoResp() {
        return com.cong.fishisland.game.model.dto.response.PlayerInfoResp.builder()
                .userId(userId)
                .userName(userName)
                .avatar(avatar)
                .ready(ready)
                .online(online)
                .isLandlord(isLandlord)
                .role(role != null ? role.name() : "PLAYER")
                .robScore(robScore)
                .cardCount(getCardCount())
                .robotControlled(robotControlled)
                .build();
    }
}
