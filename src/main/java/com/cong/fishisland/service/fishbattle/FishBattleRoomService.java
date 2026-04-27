package com.cong.fishisland.service.fishbattle;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.fishbattle.FishBattleRoom;

import java.util.List;

/**
 * 摸鱼大乱斗房间服务
 */
public interface FishBattleRoomService extends IService<FishBattleRoom> {

    /**
     * 创建房间
     */
    FishBattleRoom createRoom(String roomName, String gameMode, boolean aiFillEnabled, Long creatorId);

    /**
     * 获取等待中的房间列表
     */
    List<FishBattleRoom> listWaitingRooms();

    /**
     * 根据房间编码获取房间
     */
    FishBattleRoom getByRoomCode(String roomCode);

    /**
     * 分页查询活跃房间（等待中+对局中）
     */
    IPage<FishBattleRoom> listActiveRoomsPage(int current, int pageSize);

    /**
     * 获取正在战斗中的玩家数量（status=1的房间中的真人玩家数）
     */
    int getFightingPlayerCount();

    /**
     * 获取正在战斗中的玩家列表（userId + userName）
     */
    List<java.util.Map<String, Object>> getFightingPlayers();

    /**
     * 同步房间当前人数（根据room_player表统计）
     */
    void syncCurrentPlayers(Long roomId);

    /**
     * 房主离开处理：转移房主或解散房间
     * @return 新房主userId，null表示无转移（非房主离开或房间已解散）
     */
    Long handleOwnerLeave(Long roomId, Long ownerUserId);

    /**
     * 转移房主给房间内最早加入的其他真人玩家
     * @return 新房主userId，null表示无可转移玩家
     */
    Long transferOwner(Long roomId, Long currentOwnerId);

    /**
     * 游戏开始时将房间和玩家数据落库（选英雄阶段开始时调用）
     * @param roomCode 房间编码
     * @param roomName 房间名称
     * @param gameMode 游戏模式
     * @param maxPlayers 最大人数
     * @param currentPlayers 当前人数
     * @param aiFillEnabled 是否AI补位
     * @param creatorId 创建者ID
     * @param players 玩家列表（userId, playerName, team, slotIndex）
     * @return DB生成的房间ID
     */
    Long persistRoomOnGameStart(String roomCode, String roomName, String gameMode,
                                int maxPlayers, int currentPlayers, boolean aiFillEnabled,
                                Long creatorId, List<java.util.Map<String, Object>> players);

    /**
     * 查询进行中的房间列表（status=1选英雄中 或 status=2对局中）
     */
    List<FishBattleRoom> listInProgressRooms();

    /**
     * 查询指定用户是否有未结束的对局（status=1或2），返回最新一个房间信息
     * @return 房间信息，null表示无进行中对局
     */
    FishBattleRoom getActiveRoomByUserId(Long userId);

    /**
     * 查询指定用户所有未结束的对局房间列表（status=1或2）
     */
    List<FishBattleRoom> listActiveRoomsByUserId(Long userId);

    /**
     * 更新DB房间中玩家的英雄选择信息
     */
    void updatePlayersHeroSelection(Long dbRoomId, List<java.util.Map<String, Object>> playerHeroData);

    /**
     * 更新DB房间状态
     */
    void updateRoomStatus(Long dbRoomId, int status);

    /**
     * 清理DB中残留的进行中房间（后端重启时调用）
     * 将 status=1(选英雄中) 和 status=2(对局中) 的房间标记为 status=3(异常结束)
     * @return 清理的房间数量
     */
    int cleanupStaleRooms();
}
