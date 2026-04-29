package com.cong.fishisland.controller.fishbattle;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.entity.fishbattle.FishBattleHero;
import com.cong.fishisland.model.entity.fishbattle.FishBattlePlayerStats;
import com.cong.fishisland.model.entity.fishbattle.FishBattleUserStats;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.fishbattle.FishBattleGameService;
import com.cong.fishisland.service.fishbattle.FishBattleHeroService;
import com.cong.fishisland.service.fishbattle.FishBattlePlayerStatsService;
import com.cong.fishisland.service.fishbattle.FishBattleRoomService;
import com.cong.fishisland.service.fishbattle.FishBattleUserStatsService;
import com.cong.fishisland.socketio.FishBattleRoomManager;
import com.corundumstudio.socketio.SocketIOServer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 摸鱼大乱斗统计接口
 */
@Api(tags = "摸鱼大乱斗-统计")
@RestController
@RequestMapping("/fishBattle/stats")
@RequiredArgsConstructor
public class FishBattleStatsController {

    private final FishBattleUserStatsService fishBattleUserStatsService;
    private final FishBattlePlayerStatsService fishBattlePlayerStatsService;
    private final FishBattleHeroService fishBattleHeroService;
    private final FishBattleGameService fishBattleGameService;
    private final FishBattleRoomManager fishBattleRoomManager;
    private final FishBattleRoomService fishBattleRoomService;
    private final UserService userService;
    private final SocketIOServer fishBattleSocketIOServer;

    @ApiOperation("获取个人总体统计")
    @GetMapping("/user")
    public BaseResponse<FishBattleUserStats> getUserStats() {
        User loginUser = userService.getLoginUser();
        return ResultUtils.success(fishBattleUserStatsService.getOrInitByUserId(loginUser.getId()));
    }

    @ApiOperation("获取对局历史（分页）")
    @GetMapping("/history")
    public BaseResponse<IPage<FishBattlePlayerStats>> getHistory(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize) {
        User loginUser = userService.getLoginUser();
        IPage<FishBattlePlayerStats> page = fishBattlePlayerStatsService.pageByUserId(loginUser.getId(), current, pageSize);
        // 填充英雄中文名
        if (!page.getRecords().isEmpty()) {
            Map<String, String> heroNameMap = new HashMap<>();
            for (FishBattleHero h : fishBattleHeroService.listEnabledHeroes()) {
                heroNameMap.put(h.getHeroId(), h.getName());
            }
            for (FishBattlePlayerStats ps : page.getRecords()) {
                ps.setHeroName(heroNameMap.getOrDefault(ps.getHeroId(), ps.getHeroId()));
            }
        }
        return ResultUtils.success(page);
    }

    @ApiOperation("获取排行榜")
    @GetMapping("/leaderboard")
    public BaseResponse<List<FishBattleUserStats>> getLeaderboard(
            @RequestParam(defaultValue = "50") int limit) {
        List<FishBattleUserStats> list = fishBattleUserStatsService.getLeaderboard(limit);
        // 批量填充用户昵称和头像
        if (!list.isEmpty()) {
            List<Long> userIds = new java.util.ArrayList<>();
            for (FishBattleUserStats s : list) {
                if (s.getUserId() != null) userIds.add(s.getUserId());
            }
            if (!userIds.isEmpty()) {
                List<User> users = userService.listByIds(userIds);
                Map<Long, User> userMap = new HashMap<>();
                for (User u : users) {
                    userMap.put(u.getId(), u);
                }
                for (FishBattleUserStats s : list) {
                    User u = userMap.get(s.getUserId());
                    if (u != null) {
                        s.setUserName(u.getUserName());
                        s.setUserAvatar(u.getUserAvatar());
                    }
                }
            }
        }
        return ResultUtils.success(list);
    }

    @ApiOperation("获取指定玩家统计数据")
    @GetMapping("/user/{userId}")
    public BaseResponse<FishBattleUserStats> getOtherUserStats(@org.springframework.web.bind.annotation.PathVariable Long userId) {
        FishBattleUserStats stats = fishBattleUserStatsService.getOrInitByUserId(userId);
        // 填充昵称和头像
        User u = userService.getById(userId);
        if (u != null) {
            stats.setUserName(u.getUserName());
            stats.setUserAvatar(u.getUserAvatar());
        }
        return ResultUtils.success(stats);
    }

    @ApiOperation("获取概览数据（在线人数/总对局数/战斗中玩家）")
    @GetMapping("/overview")
    public BaseResponse<Map<String, Object>> getOverview() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("onlineCount", fishBattleRoomManager.getOnlineCount());
        overview.put("totalGames", fishBattleGameService.getTotalGameCount());
        overview.put("fightingCount", fishBattleRoomManager.getFightingPlayerCount());
        overview.put("fightingPlayers", fishBattleRoomManager.getFightingPlayersSnapshot());
        return ResultUtils.success(overview);
    }
}
