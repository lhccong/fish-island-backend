package com.cong.fishisland.service.fishbattle;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.fishbattle.FishBattleRoomPlayer;

import java.util.List;

/**
 * 摸鱼大乱斗房间玩家服务
 */
public interface FishBattleRoomPlayerService extends IService<FishBattleRoomPlayer> {

    /**
     * 获取房间内所有玩家
     */
    List<FishBattleRoomPlayer> listByRoomId(Long roomId);

    /**
     * 玩家加入房间
     */
    FishBattleRoomPlayer joinRoom(Long roomId, Long userId, String team);

    /**
     * 玩家离开房间
     */
    boolean leaveRoom(Long roomId, Long userId);

    /**
     * 玩家准备/取消准备
     */
    boolean toggleReady(Long roomId, Long userId);

    /**
     * 玩家加入房间（指定队伍和位置）
     */
    FishBattleRoomPlayer joinRoomWithSlot(Long roomId, Long userId, String team, int slotIndex);

    /**
     * 切换队伍/位置
     */
    boolean switchTeam(Long roomId, Long userId, String newTeam, int newSlotIndex);

    /**
     * 统计房间内真人玩家数
     */
    long countRealPlayers(Long roomId);

    /**
     * 删除房间内所有玩家记录
     */
    boolean removeByRoomId(Long roomId);
}
