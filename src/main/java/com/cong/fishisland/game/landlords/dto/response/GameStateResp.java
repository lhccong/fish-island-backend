package com.cong.fishisland.game.landlords.dto.response;

import com.cong.fishisland.game.landlords.enums.GamePhaseEnum;
import com.cong.fishisland.game.enums.GameTypeEnum;
import com.cong.fishisland.game.enums.RoomStateEnum;
import com.cong.fishisland.game.model.player.GamePlayer;
import com.cong.fishisland.game.landlords.model.poker.Poker;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 游戏状态响应
 * 完整描述游戏当前状态，用于同步给客户端
 *
 * @author cong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameStateResp {

    // ==================== 房间信息 ====================

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
    private RoomStateEnum roomState;

    /**
     * 游戏阶段
     */
    private GamePhaseEnum phase;

    /**
     * 房主ID
     */
    private Long ownerId;

    // ==================== 地主相关 ====================

    /**
     * 地主ID
     */
    private Long landlordId;

    /**
     * 底牌 (仅对地主可见)
     */
    private List<PokerCardVO> bottomCards;

    // ==================== 当前操作信息 ====================

    /**
     * 当前操作玩家ID (轮到的玩家)
     */
    private Long currentPlayerId;

    /**
     * 当前叫地主玩家ID (叫地主阶段)
     */
    private Long currentRobPlayerId;

    /**
     * 当前最高叫分
     */
    private Integer highestRobScore;

    /**
     * 剩余时间 (毫秒)
     */
    private Long timeLeft;

    // ==================== 最近出牌信息 ====================

    /**
     * 最近出牌区域的牌
     */
    private List<PokerCardVO> lastPlayedCards;

    /**
     * 最近出牌玩家ID
     */
    private Long lastPlayerId;

    /**
     * 最近出牌玩家名称
     */
    private String lastPlayerName;

    /**
     * 最近出牌牌型描述
     */
    private String lastPatternDesc;

    // ==================== 玩家信息 ====================

    /**
     * 玩家列表
     */
    private List<PlayerStateVO> players;

    // ==================== 手牌 (仅对拥有者可见) ====================

    /**
     * 手牌 (仅对拥有者可见)
     */
    private List<PokerCardVO> handCards;

    // ==================== 癞子相关 ====================

    /**
     * 癞子面值 (癞子模式下)
     */
    private Integer universalValue;

    /**
     * 癞子牌 (癞子模式下，仅对拥有者可见)
     */
    private List<PokerCardVO> universalCards;

    // ==================== 游戏结果 ====================

    /**
     * 游戏结果 (游戏结束时)
     */
    private GameResultVO gameResult;

    // ==================== 辅助方法 ====================

    /**
     * 创建成功响应
     */
    public static GameStateResp success(String roomId, GameTypeEnum gameType) {
        return GameStateResp.builder()
                .roomId(roomId)
                .gameType(gameType)
                .roomState(RoomStateEnum.WAITING)
                .phase(GamePhaseEnum.WAITING)
                .players(new ArrayList<>())
                .build();
    }

    // ==================== 内部类 ====================

    /**
     * 扑克牌VO (前端友好格式)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PokerCardVO {
        private String id;
        private Integer suit;      // 花色 0-3
        private Integer rank;      // 点数 3-17
        private String display;     // 显示文本
        private boolean selected;
        private boolean isUniversal;

        public static PokerCardVO from(Poker poker) {
            if (poker == null) {
                return null;
            }
            return PokerCardVO.builder()
                    .id(poker.getId())
                    .suit(poker.getType() != null ? poker.getType().getCode() : null)
                    .rank(poker.getValue() != null ? poker.getValue().getCode() : null)
                    .display(poker.getDisplayName())
                    .selected(poker.isSelected())
                    .isUniversal(poker.isUniversal())
                    .build();
        }

        public static List<PokerCardVO> fromList(List<Poker> pokers) {
            if (pokers == null || pokers.isEmpty()) {
                return new ArrayList<>();
            }
            return pokers.stream()
                    .map(PokerCardVO::from)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 玩家状态VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerStateVO {
        private Long userId;
        private String userName;
        private String avatar;
        private Integer cardCount;
        private boolean isLandlord;
        private boolean isCurrentPlayer;
        private boolean isCurrentRobPlayer;
        private boolean isReady;
        private boolean isOnline;
        private boolean isRobotControlled;
        private Integer robScore;
        private String role;
        private List<PokerCardVO> cards;
        private List<PokerCardVO> currentPlayedCards;

        public static PlayerStateVO from(GamePlayer player, Long currentPlayerId, Long currentRobPlayerId) {
            // 手牌排序（统一按斗地主规则：非癞子按降序，癞子排最后）
            List<Poker> sortedCards = null;
            if (player.getHand() != null && !player.getHand().isEmpty()) {
                sortedCards = new ArrayList<>(player.getHand().getAll());
                sortedCards.sort((a, b) -> {
                    if (a.isUniversal() != b.isUniversal()) return a.isUniversal() ? 1 : -1;
                    return b.getLandlordsSortValue() - a.getLandlordsSortValue();
                });
            }
            return PlayerStateVO.builder()
                    .userId(player.getUserId())
                    .userName(player.getUserName())
                    .avatar(player.getAvatar())
                    .cardCount(player.getCardCount())
                    .isLandlord(player.isLandlord())
                    .isCurrentPlayer(player.getUserId().equals(currentPlayerId))
                    .isCurrentRobPlayer(player.getUserId().equals(currentRobPlayerId))
                    .isReady(player.isReady())
                    .isOnline(player.isOnline())
                    .isRobotControlled(player.isRobotControlled())
                    .robScore(player.getRobScore())
                    .role(player.getRole() != null ? player.getRole().name() : "PLAYER")
                    .cards(sortedCards != null ? PokerCardVO.fromList(sortedCards) : null)
                    .currentPlayedCards(player.getCurrentPlayedCards() != null ? PokerCardVO.fromList(player.getCurrentPlayedCards()) : new ArrayList<>())
                    .build();
        }
    }

    /**
     * 游戏结果VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameResultVO {
        private Long winnerId;
        private String winnerName;
        private boolean isLandlordWin;
        private List<PlayerResultVO> players;
        private String message;
    }

    /**
     * 玩家结果VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerResultVO {
        private Long userId;
        private String userName;
        private boolean isWinner;
        private boolean isLandlord;
        private Integer scoreChange;
    }
}
