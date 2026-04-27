package com.cong.fishisland.controller.fishbattle;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.entity.fishbattle.FishBattleRoom;
import com.cong.fishisland.model.entity.fishbattle.FishBattleRoomPlayer;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.fishbattle.FishBattleRoomPlayerService;
import com.cong.fishisland.service.fishbattle.FishBattleRoomService;
import com.cong.fishisland.socketio.FishBattleRoomManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 摸鱼大乱斗房间接口
 */
@Api(tags = "摸鱼大乱斗-房间")
@RestController
@RequestMapping("/fishBattle/room")
@RequiredArgsConstructor
public class FishBattleRoomController {

    private final FishBattleRoomService fishBattleRoomService;
    private final FishBattleRoomPlayerService fishBattleRoomPlayerService;
    private final UserService userService;
    private final FishBattleRoomManager fishBattleRoomManager;

    @ApiOperation("创建房间")
    @PostMapping("/create")
    public BaseResponse<FishBattleRoom> createRoom(@RequestBody Map<String, Object> params) {
        User loginUser = userService.getLoginUser();
        String roomName = (String) params.getOrDefault("roomName", "摸鱼大乱斗房间");
        String gameMode = (String) params.getOrDefault("gameMode", "classic");
        Boolean aiFillEnabled = (Boolean) params.getOrDefault("aiFillEnabled", true);

        FishBattleRoom room = fishBattleRoomService.createRoom(roomName, gameMode, aiFillEnabled, loginUser.getId());
        return ResultUtils.success(room);
    }

    @ApiOperation("获取等待中的房间列表")
    @GetMapping("/list")
    public BaseResponse<List<FishBattleRoom>> listRooms() {
        return ResultUtils.success(fishBattleRoomService.listWaitingRooms());
    }

    @ApiOperation("分页查询活跃房间（等待中+对局中）")
    @GetMapping("/page")
    public BaseResponse<IPage<FishBattleRoom>> listRoomsPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResultUtils.success(fishBattleRoomService.listActiveRoomsPage(current, pageSize));
    }

    @ApiOperation("根据房间编码获取房间详情（含玩家列表）")
    @GetMapping("/{roomCode}")
    public BaseResponse<Map<String, Object>> getRoomDetail(@PathVariable String roomCode) {
        FishBattleRoom room = fishBattleRoomService.getByRoomCode(roomCode);
        if (room == null || room.getStatus() == 2) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "房间不存在");
        }
        List<FishBattleRoomPlayer> players = fishBattleRoomPlayerService.listByRoomId(room.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("room", room);
        result.put("players", players);
        return ResultUtils.success(result);
    }

    @ApiOperation("加入房间（选择队伍和位置）")
    @PostMapping("/join")
    public BaseResponse<FishBattleRoomPlayer> joinRoom(@RequestBody Map<String, Object> params) {
        User loginUser = userService.getLoginUser();
        String roomCode = (String) params.get("roomCode");
        String team = (String) params.get("team");
        Integer slotIndex = (Integer) params.get("slotIndex");
        if (roomCode == null || team == null || slotIndex == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不完整");
        }
        FishBattleRoom room = fishBattleRoomService.getByRoomCode(roomCode);
        if (room == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "房间不存在");
        }
        if (room.getStatus() != 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "房间不在等待状态，无法加入");
        }
        FishBattleRoomPlayer player = fishBattleRoomPlayerService.joinRoomWithSlot(
                room.getId(), loginUser.getId(), team, slotIndex);
        // 同步更新房间当前人数
        fishBattleRoomService.syncCurrentPlayers(room.getId());
        return ResultUtils.success(player);
    }

    @ApiOperation("查询当前用户进行中的对局列表（断线重连用）")
    @GetMapping("/myActiveRoom")
    public BaseResponse<List<Map<String, Object>>> getMyActiveRoom() {
        User loginUser = userService.getLoginUser();
        List<FishBattleRoom> rooms = fishBattleRoomService.listActiveRoomsByUserId(loginUser.getId());
        if (rooms.isEmpty()) {
            return ResultUtils.success(Collections.emptyList());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (FishBattleRoom room : rooms) {
            Map<String, Object> item = new HashMap<>();
            item.put("roomCode", room.getRoomCode());
            item.put("roomName", room.getRoomName());
            item.put("status", room.getStatus());
            item.put("gameMode", room.getGameMode());
            item.put("currentPlayers", room.getCurrentPlayers());
            // 从内存会话获取实时阶段信息
            FishBattleRoomManager.RoomSession session = fishBattleRoomManager.getRoomSession(room.getRoomCode());
            item.put("isLoadingPhase", session != null && session.isLoadingPhase());
            result.add(item);
        }
        return ResultUtils.success(result);
    }

    @ApiOperation("查询进行中的房间列表（大厅用，status=1或2）")
    @GetMapping("/inProgress")
    public BaseResponse<List<FishBattleRoom>> listInProgressRooms() {
        return ResultUtils.success(fishBattleRoomService.listInProgressRooms());
    }

    @ApiOperation("切换队伍/位置")
    @PostMapping("/switchTeam")
    public BaseResponse<Boolean> switchTeam(@RequestBody Map<String, Object> params) {
        User loginUser = userService.getLoginUser();
        String roomCode = (String) params.get("roomCode");
        String newTeam = (String) params.get("team");
        Integer newSlotIndex = (Integer) params.get("slotIndex");
        if (roomCode == null || newTeam == null || newSlotIndex == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不完整");
        }
        FishBattleRoom room = fishBattleRoomService.getByRoomCode(roomCode);
        if (room == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "房间不存在");
        }
        boolean success = fishBattleRoomPlayerService.switchTeam(
                room.getId(), loginUser.getId(), newTeam, newSlotIndex);
        if (!success) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "切换失败，该位置可能已被占用");
        }
        return ResultUtils.success(true);
    }
}
