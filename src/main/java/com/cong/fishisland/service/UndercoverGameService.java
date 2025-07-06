package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.game.UndercoverGuessRequest;
import com.cong.fishisland.model.dto.game.UndercoverRoomCreateRequest;
import com.cong.fishisland.model.dto.game.UndercoverVoteRequest;
import com.cong.fishisland.model.vo.game.UndercoverPlayerDetailVO;
import com.cong.fishisland.model.vo.game.UndercoverPlayerVO;
import com.cong.fishisland.model.vo.game.UndercoverRoomVO;
import com.cong.fishisland.model.vo.game.UndercoverVoteVO;

import java.util.List;

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
     * 获取所有房间列表
     *
     * @return 房间列表
     */
    List<UndercoverRoomVO> getAllRooms();
    
    /**
     * 根据房间ID获取房间信息
     *
     * @param roomId 房间ID
     * @return 房间信息
     */
    UndercoverRoomVO getRoomById(String roomId);

    /**
     * 加入游戏房间
     *
     * @param roomId 房间ID
     * @return 是否成功加入
     */
    boolean joinRoom(String roomId);

    /**
     * 退出游戏房间
     *
     * @param roomId 房间ID
     * @return 是否成功退出
     */
    boolean quitRoom(String roomId);

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
     * 移除当前活跃房间（仅管理员）
     *
     * @return 是否成功移除
     */
    boolean removeActiveRoom(String roomId);

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
     * @param userId 用户ID
     * @return 是否成功淘汰
     */
    boolean eliminatePlayer(String roomId, Long userId);

    /**
     * 检查游戏是否结束
     *
     * @param roomId 房间ID
     * @return 是否结束
     */
    boolean checkGameOver(String roomId);

    /**
     * 获取房间投票记录
     *
     * @param roomId 房间ID
     * @return 投票记录
     */
    List<UndercoverVoteVO> getRoomVotes(String roomId);

    /**
     * 投票
     *
     * @param request 投票请求
     * @return 是否成功投票
     */
    boolean vote(UndercoverVoteRequest request);

    /**
     * 获取玩家详细信息
     *
     * @param roomId 房间ID
     * @param userId 用户ID
     * @return 玩家详细信息
     */
    UndercoverPlayerDetailVO getPlayerDetailInfo(String roomId, Long userId);

    /**
     * 获取房间内所有玩家详细信息
     *
     * @param roomId 房间ID
     * @return 玩家详细信息列表
     */
    List<UndercoverPlayerDetailVO> getRoomPlayersDetail(String roomId);

    /**
     * 猜词（卧底猜词模式）
     *
     * @param request 猜词请求
     * @return 是否猜对
     */
    boolean guessWord(UndercoverGuessRequest request);

    /**
     * 按房间存活玩家顺序依次发送发言提醒，发送间隔20秒，全部玩家发送完毕后提醒投票，投票时间30秒后自动结算
     *
     * @param roomId 房间ID
     */
    void startSpeakingAndVoting(String roomId);
} 