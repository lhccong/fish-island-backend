package com.cong.fishisland.controller.game;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.game.DrawDataSaveRequest;
import com.cong.fishisland.model.dto.game.DrawGuessRequest;
import com.cong.fishisland.model.dto.game.DrawRoomCreateRequest;
import com.cong.fishisland.model.vo.game.DrawGuessVO;
import com.cong.fishisland.model.vo.game.DrawRoomVO;
import com.cong.fishisland.service.DrawGameService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 你画我猜游戏接口
 *
 * @author cong
 */
@RestController
@RequestMapping("/draw")
@Slf4j
public class DrawGameController {

    @Resource
    private DrawGameService drawGameService;

    /**
     * 创建房间
     *
     * @param request 创建房间请求
     * @return 房间ID
     */
    @PostMapping("/room/create")
    public BaseResponse<String> createRoom(@RequestBody DrawRoomCreateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String roomId = drawGameService.createRoom(request);
        return ResultUtils.success(roomId);
    }

    /**
     * 加入房间
     *
     * @param roomId 房间ID
     * @return 是否成功
     */
    @PostMapping("/room/join")
    public BaseResponse<Boolean> joinRoom(@RequestParam String roomId) {
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = drawGameService.joinRoom(roomId);
        return ResultUtils.success(result);
    }

    /**
     * 退出房间
     *
     * @param roomId 房间ID
     * @return 是否成功
     */
    @PostMapping("/room/quit")
    public BaseResponse<Boolean> quitRoom(@RequestParam String roomId) {
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = drawGameService.quitRoom(roomId);
        return ResultUtils.success(result);
    }

    /**
     * 开始游戏
     *
     * @param roomId 房间ID
     * @return 是否成功
     */
    @PostMapping("/game/start")
    public BaseResponse<Boolean> startGame(@RequestParam String roomId) {
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = drawGameService.startGame(roomId);
        return ResultUtils.success(result);
    }

    /**
     * 结束游戏
     *
     * @param roomId 房间ID
     * @return 是否成功
     */
    @PostMapping("/game/end")
    public BaseResponse<Boolean> endGame(@RequestParam String roomId) {
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = drawGameService.endGame(roomId);
        return ResultUtils.success(result);
    }

    /**
     * 保存绘画数据
     *
     * @param request 保存绘画数据请求
     * @return 是否成功
     */
    @PostMapping("/data/save")
    public BaseResponse<Boolean> saveDrawData(@RequestBody DrawDataSaveRequest request) {
        if (request == null || StringUtils.isBlank(request.getRoomId()) || StringUtils.isBlank(request.getDrawData())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = drawGameService.saveDrawData(request);
        return ResultUtils.success(result);
    }

    /**
     * 猜词
     *
     * @param request 猜词请求
     * @return 猜词结果
     */
    @PostMapping("/guess")
    public BaseResponse<DrawGuessVO> guessWord(@RequestBody DrawGuessRequest request) {
        if (request == null || StringUtils.isBlank(request.getRoomId()) || StringUtils.isBlank(request.getGuessWord())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        DrawGuessVO result = drawGameService.guessWord(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取房间信息
     *
     * @param roomId 房间ID
     * @return 房间信息
     */
    @GetMapping("/room/{roomId}")
    public BaseResponse<DrawRoomVO> getRoomById(@PathVariable String roomId) {
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        DrawRoomVO roomVO = drawGameService.getRoomById(roomId);
        return ResultUtils.success(roomVO);
    }

    /**
     * 获取所有房间列表
     *
     * @return 房间列表
     */
    @GetMapping("/room/list")
    public BaseResponse<List<DrawRoomVO>> getAllRooms() {
        List<DrawRoomVO> roomList = drawGameService.getAllRooms();
        return ResultUtils.success(roomList);
    }

    /**
     * 获取房间的猜词记录
     *
     * @param roomId 房间ID
     * @return 猜词记录列表
     */
    @GetMapping("/room/{roomId}/guesses")
    public BaseResponse<List<DrawGuessVO>> getRoomGuesses(@PathVariable String roomId) {
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<DrawGuessVO> guesses = drawGameService.getRoomGuesses(roomId);
        return ResultUtils.success(guesses);
    }

    /**
     * 移除房间（管理员专用）
     *
     * @param roomId 房间ID
     * @return 是否成功
     */
    @PostMapping("/room/remove")
    public BaseResponse<Boolean> removeRoom(@RequestParam String roomId) {
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = drawGameService.removeRoom(roomId);
        return ResultUtils.success(result);
    }
    
    /**
     * 进入下一轮游戏
     *
     * @param roomId 房间ID
     * @return 是否成功
     */
    @PostMapping("/game/next-round")
    public BaseResponse<Boolean> nextRound(@RequestParam String roomId) {
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = drawGameService.nextRound(roomId);
        return ResultUtils.success(result);
    }
} 