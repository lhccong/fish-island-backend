package com.cong.fishisland.model.fishbattle.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 战斗房间运行时状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleRoom {
    /**
     * 房间 ID。
     */
    private String roomId;

    /**
     * 创建时间戳。
     */
    private Long createdAt;

    /**
     * 房间内序列号发生器。
     */
    private AtomicLong sequence;

    /**
     * 当前战斗已运行时长，单位秒。
     */
    private Double gameTimer;

    /**
     * 蓝队总击杀数。
     */
    private Integer blueKills;

    /**
     * 红队总击杀数。
     */
    private Integer redKills;

    /**
     * 服务端 tick 帧号（从 0 单调递增），随快照广播给客户端。
     * 客户端用于区分快照时序、对齐插值。
     */
    private Long tickNumber;

    /**
     * 当前房间全部英雄状态。
     */
    private List<BattleChampionState> champions;

    /**
     * 当前房间全部小兵状态。
     */
    private List<BattleMinionState> minions;

    /**
     * 当前房间全部建筑状态。
     */
    private List<BattleStructureState> structures;

    /**
     * 当前房间全部补血道具状态。
     */
    private List<BattleHealthRelicState> healthRelics;

    /**
     * 关联的 DB 房间 ID（fish_battle_room.id）。
     */
    private Long dbRoomId;

    /**
     * 游戏阶段：playing / ended。
     */
    private String gamePhase;

    /**
     * 获胜队伍：blue / red，仅 gamePhase=ended 时有值。
     */
    private String winnerTeam;

    /**
     * 游戏结束时间戳，用于结束后延迟清理。
     */
    private Long gameEndAt;

    /**
     * 下一波小兵生成时间戳，到达后生成一波并重新计时。
     */
    private Long nextMinionSpawnAt;

    private Long minionWaveSequence;

    /**
     * 延迟伤害队列（塔弹道、远程小兵弹道等），tick 结束统一结算。
     */
    private List<PendingAttackState> pendingAttacks;

    /**
     * 当前房间在线会话。
     */
    private List<PlayerSession> players;

    public static BattleRoom empty(String roomId) {
        return BattleRoom.builder()
                .roomId(roomId)
                .createdAt(System.currentTimeMillis())
                .sequence(new AtomicLong(0))
                .gameTimer(0D)
                .blueKills(0)
                .redKills(0)
                .tickNumber(0L)
                .gamePhase("playing")
                .winnerTeam(null)
                .gameEndAt(null)
                .champions(new ArrayList<BattleChampionState>())
                .minions(new ArrayList<BattleMinionState>())
                .structures(new ArrayList<BattleStructureState>())
                .healthRelics(new ArrayList<BattleHealthRelicState>())
                .nextMinionSpawnAt(System.currentTimeMillis() + 5000L)
                .minionWaveSequence(0L)
                .pendingAttacks(new ArrayList<PendingAttackState>())
                .players(new CopyOnWriteArrayList<PlayerSession>())
                .build();
    }
}
