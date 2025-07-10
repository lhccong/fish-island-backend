package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.game.DrawDataSaveRequest;
import com.cong.fishisland.model.dto.game.DrawGuessRequest;
import com.cong.fishisland.model.dto.game.DrawRoomCreateRequest;
import com.cong.fishisland.model.vo.game.DrawGuessVO;
import com.cong.fishisland.model.vo.game.DrawRoomVO;

import java.util.List;

/**
 * 你画我猜游戏服务接口
 *
 * @author cong
 */
public interface DrawGameService {

    /**
     * 创建房间
     *
     * @param request 创建房间请求
     * @return 房间ID
     */
    String createRoom(DrawRoomCreateRequest request);

    /**
     * 加入房间
     *
     * @param roomId 房间ID
     * @return 是否成功
     */
    boolean joinRoom(String roomId);

    /**
     * 退出房间
     *
     * @param roomId 房间ID
     * @return 是否成功
     */
    boolean quitRoom(String roomId);

    /**
     * 开始游戏
     *
     * @param roomId 房间ID
     * @return 是否成功
     */
    boolean startGame(String roomId);

    /**
     * 结束游戏
     *
     * @param roomId 房间ID
     * @return 是否成功
     */
    boolean endGame(String roomId);

    /**
     * 保存绘画数据
     *
     * @param request 保存绘画数据请求
     * @return 是否成功
     */
    boolean saveDrawData(DrawDataSaveRequest request);

    /**
     * 猜词
     *
     * @param request 猜词请求
     * @return 猜词结果
     */
    DrawGuessVO guessWord(DrawGuessRequest request);

    /**
     * 获取房间信息
     *
     * @param roomId 房间ID
     * @return 房间信息
     */
    DrawRoomVO getRoomById(String roomId);

    /**
     * 获取所有房间列表
     *
     * @return 房间列表
     */
    List<DrawRoomVO> getAllRooms();

    /**
     * 获取房间的猜词记录
     *
     * @param roomId 房间ID
     * @return 猜词记录列表
     */
    List<DrawGuessVO> getRoomGuesses(String roomId);

    /**
     * 移除房间（管理员专用）
     *
     * @param roomId 房间ID
     * @return 是否成功
     */
    boolean removeRoom(String roomId);
} 