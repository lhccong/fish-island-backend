package com.cong.fishisland.controller.game;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.game.UndercoverRoomCreateRequest;
import com.cong.fishisland.model.dto.game.UndercoverRoomJoinRequest;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.game.UndercoverPlayerVO;
import com.cong.fishisland.model.vo.game.UndercoverRoomVO;
import com.cong.fishisland.service.UndercoverGameService;
import com.cong.fishisland.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 谁是卧底游戏控制器
 *
 * @author cong
 */
@RestController
@RequestMapping("/undercover")
@Slf4j
@Api(tags = "谁是卧底游戏")
public class UndercoverGameController {

    @Resource
    private UndercoverGameService undercoverGameService;

    @Resource
    private UserService userService;

    /**
     * 创建游戏房间（仅管理员）
     *
     * @param request 创建房间请求
     * @return 房间ID
     */
    @PostMapping("/room/create")
    @ApiOperation(value = "创建游戏房间（仅管理员）")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<String> createRoom(@RequestBody UndercoverRoomCreateRequest request) {
        if (StringUtils.isBlank(request.getCivilianWord())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "平民词语不能为空");
        }
        if (StringUtils.isBlank(request.getUndercoverWord())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "卧底词语不能为空");
        }
        if (request.getDuration() == null || request.getDuration() < 60) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "游戏持续时间不能少于60秒");
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
    public BaseResponse<UndercoverRoomVO> getActiveRoom() {
        UndercoverRoomVO roomVO = undercoverGameService.getActiveRoom();
        return ResultUtils.success(roomVO);
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
     * 开始游戏（仅管理员）
     *
     * @param roomId 房间ID
     * @return 是否成功
     */
    @PostMapping("/room/start")
    @ApiOperation(value = "开始游戏（仅管理员）")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> startGame(@RequestParam String roomId) {
        // 手动验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        
        boolean result = undercoverGameService.startGame(roomId);
        return ResultUtils.success(result);
    }

    /**
     * 结束游戏（仅管理员）
     *
     * @param roomId 房间ID
     * @return 是否成功
     */
    @PostMapping("/room/end")
    @ApiOperation(value = "结束游戏（仅管理员）")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> endGame(@RequestParam String roomId) {
        // 手动验证参数
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
     * @return 玩家信息
     */
    @GetMapping("/room/player")
    @ApiOperation(value = "获取当前玩家信息")
    public BaseResponse<UndercoverPlayerVO> getCurrentPlayerInfo(@RequestParam String roomId) {
        // 手动验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        
        User loginUser = userService.getLoginUser();
        UndercoverPlayerVO playerVO = undercoverGameService.getPlayerInfo(roomId, loginUser.getId());
        return ResultUtils.success(playerVO);
    }

    /**
     * 获取指定玩家信息（仅管理员）
     *
     * @param roomId 房间ID
     * @param userId 用户ID
     * @return 玩家信息
     */
    @GetMapping("/admin/room/player")
    @ApiOperation(value = "获取指定玩家信息（仅管理员）")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<UndercoverPlayerVO> getPlayerInfo(@RequestParam String roomId, @RequestParam Long userId) {
        // 手动验证参数
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
     * 淘汰玩家
     *
     * @param roomId 房间ID
     * @param userId 被淘汰的用户ID
     * @return 是否成功
     */
    @PostMapping("/room/eliminate")
    @ApiOperation(value = "淘汰玩家")
    public BaseResponse<Boolean> eliminatePlayer(@RequestParam String roomId, @RequestParam Long userId) {
        // 手动验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }
        
        // 验证用户登录状态
        User loginUser = userService.getLoginUser();
        
        // 只有管理员可以淘汰任何玩家，普通用户只能投票（这里简化为管理员直接淘汰）
        if (!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有管理员可以淘汰玩家");
        }
        
        boolean result = undercoverGameService.eliminatePlayer(roomId, userId);
        return ResultUtils.success(result);
    }

    /**
     * 检查游戏是否结束
     *
     * @param roomId 房间ID
     * @return 游戏是否结束
     */
    @GetMapping("/room/check-game-over")
    @ApiOperation(value = "检查游戏是否结束")
    public BaseResponse<Boolean> checkGameOver(@RequestParam String roomId) {
        // 手动验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        
        boolean isGameOver = undercoverGameService.checkGameOver(roomId);
        return ResultUtils.success(isGameOver);
    }
} 