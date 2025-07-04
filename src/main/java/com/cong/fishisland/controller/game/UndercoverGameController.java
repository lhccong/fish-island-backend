package com.cong.fishisland.controller.game;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.game.UndercoverGuessRequest;
import com.cong.fishisland.model.dto.game.UndercoverRoomCreateRequest;
import com.cong.fishisland.model.dto.game.UndercoverRoomJoinRequest;
import com.cong.fishisland.model.dto.game.UndercoverRoomQuitRequest;
import com.cong.fishisland.model.dto.game.UndercoverVoteRequest;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.game.UndercoverPlayerDetailVO;
import com.cong.fishisland.model.vo.game.UndercoverPlayerVO;
import com.cong.fishisland.model.vo.game.UndercoverRoomVO;
import com.cong.fishisland.model.vo.game.UndercoverVoteVO;
import com.cong.fishisland.service.UndercoverGameService;
import com.cong.fishisland.service.UserService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 谁是卧底游戏控制器
 *
 * @author cong
 */
@RestController
@RequestMapping("/undercover")
@Slf4j
//@Api(tags = "谁是卧底游戏")
public class UndercoverGameController {

    @Resource
    private UndercoverGameService undercoverGameService;

    @Resource
    private UserService userService;

    /**
     * 创建游戏房间
     *
     * @param request 创建房间请求
     * @return 房间ID
     */
    @PostMapping("/room/create")
    @ApiOperation(value = "创建游戏房间")
    public BaseResponse<String> createRoom(@RequestBody UndercoverRoomCreateRequest request) {
        // 验证基本参数
        if (request.getDuration() == null || request.getDuration() < 60) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "游戏持续时间不能少于60秒");
        }
        if (request.getMaxPlayers() == null || request.getMaxPlayers() < 3) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间最大人数不能少于3人");
        }
        if (request.getMaxPlayers() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间最大人数不能超过20人");
        }
        
        String roomId = undercoverGameService.createRoom(request);
        return ResultUtils.success(roomId);
    }

    /**
     * 获取当前活跃房间
     *
     * @return 房间信息
     */
    @GetMapping("/room/active")
    @ApiOperation(value = "获取当前活跃房间")
    public BaseResponse<UndercoverRoomVO> getActiveRoom(String roomId) {
        UndercoverRoomVO roomVO = undercoverGameService.getRoomById(roomId);
        return ResultUtils.success(roomVO);
    }

    /**
     * 移除当前活跃房间（仅管理员）
     *
     * @return 是否成功
     */
    @PostMapping("/room/remove")
    @ApiOperation(value = "移除当前活跃房间（仅管理员）")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> removeActiveRoom() {
        boolean result = undercoverGameService.removeActiveRoom();
        return ResultUtils.success(result);
    }

    /**
     * 加入游戏房间
     *
     * @param request 加入房间请求
     * @return 是否成功
     */
    @PostMapping("/room/join")
    @ApiOperation(value = "加入游戏房间")
    public BaseResponse<Boolean> joinRoom(@RequestBody UndercoverRoomJoinRequest request) {
        // 手动验证参数
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        if (StringUtils.isBlank(request.getRoomId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        
        boolean result = undercoverGameService.joinRoom(request.getRoomId());
        return ResultUtils.success(result);
    }

    /**
     * 退出游戏房间
     *
     * @param request 退出房间请求
     * @return 是否成功
     */
    @PostMapping("/room/quit")
    @ApiOperation(value = "退出游戏房间")
    public BaseResponse<Boolean> quitRoom(@RequestBody UndercoverRoomQuitRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        
        boolean result = undercoverGameService.quitRoom(request.getRoomId());
        return ResultUtils.success(result);
    }

    /**
     * 开始游戏
     *
     * @param roomId 房间ID
     * @return 是否成功
     */
    @PostMapping("/room/start")
    @ApiOperation(value = "开始游戏")
    public BaseResponse<Boolean> startGame(@RequestParam String roomId) {
        // 验证基本参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        
        boolean result = undercoverGameService.startGame(roomId);
        return ResultUtils.success(result);
    }

    /**
     * 结束游戏
     *
     * @param roomId 房间ID
     * @return 是否成功
     */
    @PostMapping("/room/end")
    @ApiOperation(value = "结束游戏")
    public BaseResponse<Boolean> endGame(@RequestParam String roomId) {
        // 验证基本参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        
        boolean result = undercoverGameService.endGame(roomId);
        return ResultUtils.success(result);
    }

    /**
     * 获取玩家信息
     *
     * @param roomId 房间ID
     * @param userId 用户ID
     * @return 玩家信息
     */
    @GetMapping("/player/info")
    @ApiOperation(value = "获取玩家信息")
    public BaseResponse<UndercoverPlayerVO> getPlayerInfo(@RequestParam String roomId, @RequestParam Long userId) {
        // 验证基本参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }
        
        UndercoverPlayerVO playerVO = undercoverGameService.getPlayerInfo(roomId, userId);
        return ResultUtils.success(playerVO);
    }

    /**
     * 获取当前登录用户信息
     *
     * @param roomId 房间ID
     * @return 玩家信息
     */
    @GetMapping("/player/current")
    @ApiOperation(value = "获取当前登录用户信息")
    public BaseResponse<UndercoverPlayerVO> getCurrentPlayerInfo(@RequestParam String roomId) {
        // 验证基本参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        
        User loginUser = userService.getLoginUser();
        UndercoverPlayerVO playerVO = undercoverGameService.getPlayerInfo(roomId, loginUser.getId());
        return ResultUtils.success(playerVO);
    }

    /**
     * 获取玩家详细信息
     *
     * @param roomId 房间ID
     * @param userId 用户ID
     * @return 玩家详细信息
     */
    @GetMapping("/player/detail")
    @ApiOperation(value = "获取玩家详细信息")
    public BaseResponse<UndercoverPlayerDetailVO> getPlayerDetailInfo(@RequestParam String roomId, @RequestParam Long userId) {
        // 验证基本参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }
        
        UndercoverPlayerDetailVO playerDetailVO = undercoverGameService.getPlayerDetailInfo(roomId, userId);
        return ResultUtils.success(playerDetailVO);
    }

    /**
     * 淘汰玩家
     *
     * @param roomId 房间ID
     * @param userId 被淘汰的用户ID
     * @return 是否成功
     */
    @PostMapping("/player/eliminate")
    @ApiOperation(value = "淘汰玩家")
    public BaseResponse<Boolean> eliminatePlayer(@RequestParam String roomId, @RequestParam Long userId) {
        // 验证基本参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }
        
        boolean result = undercoverGameService.eliminatePlayer(roomId, userId);
        return ResultUtils.success(result);
    }

    /**
     * 获取投票结果
     *
     * @param roomId 房间ID
     * @return 投票结果
     */
    @GetMapping("/room/votes")
    @ApiOperation(value = "获取投票结果")
    public BaseResponse<List<UndercoverVoteVO>> getRoomVotes(@RequestParam String roomId) {

        List<UndercoverVoteVO> votes = undercoverGameService.getRoomVotes(roomId);
        return ResultUtils.success(votes);
    }

    /**
     * 提交投票
     *
     * @param request 投票请求
     * @return 是否成功
     */
    @PostMapping("/room/vote")
    @ApiOperation(value = "提交投票")
    public BaseResponse<Boolean> vote(@RequestBody UndercoverVoteRequest request) {
        // 手动验证参数
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        if (request.getTargetId() == null || request.getTargetId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "被投票的用户ID不合法");
        }
        
        boolean result = undercoverGameService.vote(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取房间内所有玩家详细信息
     *
     * @param roomId 房间ID
     * @return 玩家详细信息列表
     */
    @GetMapping("/room/players-detail")
    @ApiOperation(value = "获取房间内所有玩家详细信息")
    public BaseResponse<List<UndercoverPlayerDetailVO>> getRoomPlayersDetail(@RequestParam String roomId) {

        List<UndercoverPlayerDetailVO> playersDetail = undercoverGameService.getRoomPlayersDetail(roomId);
        return ResultUtils.success(playersDetail);
    }

    /**
     * 卧底猜平民词
     *
     * @param request 猜词请求
     * @return 是否猜对
     */
    @PostMapping("/room/guess")
    @ApiOperation(value = "卧底猜平民词")
    public BaseResponse<Boolean> guessWord(@RequestBody UndercoverGuessRequest request) {
        // 手动验证参数
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        if (StringUtils.isBlank(request.getGuessWord())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "猜测词语不能为空");
        }
        
        boolean result = undercoverGameService.guessWord(request);
        return ResultUtils.success(result);
    }
} 