package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.game.UndercoverRoomCreateRequest;
import com.cong.fishisland.model.entity.game.UndercoverRoom;
import com.cong.fishisland.model.vo.game.UndercoverPlayerVO;
import com.cong.fishisland.model.vo.game.UndercoverRoomVO;

/**
 * 谁是卧底游戏服务接口
 *
 * @author cong
 */
public interface UndercoverGameService {

    /**
     * 创建游戏房间（仅管理员）
     *
     * @param request 创建房间请求
     * @return 房间ID
     */
    String createRoom(UndercoverRoomCreateRequest request);

    /**
     * 获取当前活跃房间
     *
     * @return 房间信息，如果没有活跃房间则返回null
     */
    UndercoverRoomVO getActiveRoom();

    /**
     * 加入游戏房间
     *
     * @param roomId 房间ID
     * @return 是否成功加入
     */
    boolean joinRoom(String roomId);

    /**
     * 开始游戏（仅管理员）
     *
     * @param roomId 房间ID
     * @return 是否成功开始
     */
    boolean startGame(String roomId);

    /**
     * 结束游戏（仅管理员）
     *
     * @param roomId 房间ID
     * @return 是否成功结束
     */
    boolean endGame(String roomId);

    /**
     * 获取玩家信息
     *
     * @param roomId 房间ID
     * @param userId 用户ID
     * @return 玩家信息
     */
    UndercoverPlayerVO getPlayerInfo(String roomId, Long userId);

    /**
     * 淘汰玩家
     *
     * @param roomId 房间ID
     * @param userId 被淘汰的用户ID
     * @return 是否成功淘汰
     */
    boolean eliminatePlayer(String roomId, Long userId);

    /**
     * 检查游戏是否结束
     *
     * @param roomId 房间ID
     * @return 游戏是否结束
     */
    boolean checkGameOver(String roomId);
} 