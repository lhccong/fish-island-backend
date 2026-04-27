package com.cong.fishisland.service.fishbattle.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.fishbattle.FishBattleRoomMapper;
import com.cong.fishisland.model.entity.fishbattle.FishBattleRoom;
import com.cong.fishisland.model.entity.fishbattle.FishBattleRoomPlayer;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.fishbattle.FishBattleRoomPlayerService;
import com.cong.fishisland.service.fishbattle.FishBattleRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 摸鱼大乱斗房间服务实现
 */
@Service
@RequiredArgsConstructor
public class FishBattleRoomServiceImpl extends ServiceImpl<FishBattleRoomMapper, FishBattleRoom>
        implements FishBattleRoomService {

    private final FishBattleRoomPlayerService fishBattleRoomPlayerService;
    private final UserService userService;

    @Override
    public FishBattleRoom createRoom(String roomName, String gameMode, boolean aiFillEnabled, Long creatorId) {
        FishBattleRoom room = new FishBattleRoom();
        room.setRoomCode(generateRoomCode());
        room.setRoomName(roomName);
        room.setStatus(0);
        room.setGameMode(gameMode);
        room.setMaxPlayers(10);
        room.setCurrentPlayers(0);
        room.setAiFillEnabled(aiFillEnabled ? 1 : 0);
        room.setCreatorId(creatorId);
        this.save(room);
        return room;
    }

    @Override
    public List<FishBattleRoom> listWaitingRooms() {
        return this.list(new LambdaQueryWrapper<FishBattleRoom>()
                .eq(FishBattleRoom::getStatus, 0)
                .orderByDesc(FishBattleRoom::getCreateTime));
    }

    @Override
    public FishBattleRoom getByRoomCode(String roomCode) {
        return this.getOne(new LambdaQueryWrapper<FishBattleRoom>()
                .eq(FishBattleRoom::getRoomCode, roomCode));
    }

    @Override
    public IPage<FishBattleRoom> listActiveRoomsPage(int current, int pageSize) {
        IPage<FishBattleRoom> page = this.page(new Page<>(current, pageSize),
                new LambdaQueryWrapper<FishBattleRoom>()
                        .in(FishBattleRoom::getStatus, 0, 1)
                        .orderByAsc(FishBattleRoom::getStatus)
                        .orderByDesc(FishBattleRoom::getCreateTime));
        // 批量填充 creatorName
        List<Long> creatorIds = page.getRecords().stream()
                .map(FishBattleRoom::getCreatorId).distinct().collect(Collectors.toList());
        if (!creatorIds.isEmpty()) {
            Map<Long, String> nameMap = userService.listByIds(creatorIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u.getUserName() != null ? u.getUserName() : "未知玩家"));
            page.getRecords().forEach(r -> r.setCreatorName(nameMap.getOrDefault(r.getCreatorId(), "未知玩家")));
        }
        return page;
    }

    @Override
    public int getFightingPlayerCount() {
        List<FishBattleRoom> fightingRooms = this.list(new LambdaQueryWrapper<FishBattleRoom>()
                .eq(FishBattleRoom::getStatus, 1)
                .select(FishBattleRoom::getId));
        if (fightingRooms.isEmpty()) {
            return 0;
        }
        List<Long> roomIds = fightingRooms.stream().map(FishBattleRoom::getId).collect(Collectors.toList());
        return (int) fishBattleRoomPlayerService.count(new LambdaQueryWrapper<FishBattleRoomPlayer>()
                .in(FishBattleRoomPlayer::getRoomId, roomIds)
                .eq(FishBattleRoomPlayer::getIsAi, 0));
    }

    @Override
    public List<Map<String, Object>> getFightingPlayers() {
        List<FishBattleRoom> fightingRooms = this.list(new LambdaQueryWrapper<FishBattleRoom>()
                .eq(FishBattleRoom::getStatus, 1)
                .select(FishBattleRoom::getId));
        if (fightingRooms.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roomIds = fightingRooms.stream().map(FishBattleRoom::getId).collect(Collectors.toList());
        List<FishBattleRoomPlayer> players = fishBattleRoomPlayerService.list(
                new LambdaQueryWrapper<FishBattleRoomPlayer>()
                        .in(FishBattleRoomPlayer::getRoomId, roomIds)
                        .eq(FishBattleRoomPlayer::getIsAi, 0)
                        .isNotNull(FishBattleRoomPlayer::getUserId));
        if (players.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = players.stream().map(FishBattleRoomPlayer::getUserId).distinct().collect(Collectors.toList());
        Map<Long, String> userNameMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getUserName() != null ? u.getUserName() : "未知玩家"));
        return players.stream().map(p -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", p.getUserId());
            item.put("userName", userNameMap.getOrDefault(p.getUserId(), "未知玩家"));
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    public void syncCurrentPlayers(Long roomId) {
        long count = fishBattleRoomPlayerService.count(new LambdaQueryWrapper<FishBattleRoomPlayer>()
                .eq(FishBattleRoomPlayer::getRoomId, roomId));
        FishBattleRoom room = new FishBattleRoom();
        room.setId(roomId);
        room.setCurrentPlayers((int) count);
        this.updateById(room);
    }

    @Override
    public Long handleOwnerLeave(Long roomId, Long ownerUserId) {
        FishBattleRoom room = this.getById(roomId);
        if (room == null) {
            return null;
        }
        if (!ownerUserId.equals(room.getCreatorId())) {
            return null;
        }
        List<FishBattleRoomPlayer> remainingPlayers = fishBattleRoomPlayerService.list(
                new LambdaQueryWrapper<FishBattleRoomPlayer>()
                        .eq(FishBattleRoomPlayer::getRoomId, roomId)
                        .eq(FishBattleRoomPlayer::getIsAi, 0)
                        .ne(FishBattleRoomPlayer::getUserId, ownerUserId)
                        .orderByAsc(FishBattleRoomPlayer::getCreateTime));
        if (!remainingPlayers.isEmpty()) {
            Long newOwnerId = remainingPlayers.get(0).getUserId();
            room.setCreatorId(newOwnerId);
            this.updateById(room);
            return newOwnerId;
        } else {
            room.setStatus(2);
            room.setCurrentPlayers(0);
            this.updateById(room);
            fishBattleRoomPlayerService.remove(new LambdaQueryWrapper<FishBattleRoomPlayer>()
                    .eq(FishBattleRoomPlayer::getRoomId, roomId));
            return null;
        }
    }

    @Override
    public Long transferOwner(Long roomId, Long currentOwnerId) {
        FishBattleRoom room = this.getById(roomId);
        if (room == null || !currentOwnerId.equals(room.getCreatorId())) {
            return null;
        }
        List<FishBattleRoomPlayer> remainingPlayers = fishBattleRoomPlayerService.list(
                new LambdaQueryWrapper<FishBattleRoomPlayer>()
                        .eq(FishBattleRoomPlayer::getRoomId, roomId)
                        .eq(FishBattleRoomPlayer::getIsAi, 0)
                        .ne(FishBattleRoomPlayer::getUserId, currentOwnerId)
                        .orderByAsc(FishBattleRoomPlayer::getCreateTime));
        if (remainingPlayers.isEmpty()) {
            return null;
        }
        Long newOwnerId = remainingPlayers.get(0).getUserId();
        room.setCreatorId(newOwnerId);
        this.updateById(room);
        return newOwnerId;
    }

    @Override
    public Long persistRoomOnGameStart(String roomCode, String roomName, String gameMode,
                                        int maxPlayers, int currentPlayers, boolean aiFillEnabled,
                                        Long creatorId, List<Map<String, Object>> players) {
        FishBattleRoom room = new FishBattleRoom();
        room.setRoomCode(roomCode);
        room.setRoomName(roomName);
        room.setGameMode(gameMode);
        room.setMaxPlayers(maxPlayers);
        room.setCurrentPlayers(currentPlayers);
        room.setAiFillEnabled(aiFillEnabled ? 1 : 0);
        room.setCreatorId(creatorId);
        room.setStatus(1);
        this.save(room);

        Long dbRoomId = room.getId();

        for (Map<String, Object> p : players) {
            FishBattleRoomPlayer record = new FishBattleRoomPlayer();
            record.setRoomId(dbRoomId);
            record.setUserId((Long) p.get("userId"));
            record.setPlayerName((String) p.get("playerName"));
            record.setTeam((String) p.get("team"));
            record.setSlotIndex((Integer) p.get("slotIndex"));
            record.setIsReady(1);
            record.setIsAi(0);
            record.setCreateTime(new Date());
            fishBattleRoomPlayerService.save(record);
        }

        return dbRoomId;
    }

    @Override
    public List<FishBattleRoom> listInProgressRooms() {
        List<FishBattleRoom> rooms = this.list(new LambdaQueryWrapper<FishBattleRoom>()
                .in(FishBattleRoom::getStatus, 1, 2)
                .orderByDesc(FishBattleRoom::getCreateTime));
        if (!rooms.isEmpty()) {
            List<Long> creatorIds = rooms.stream()
                    .map(FishBattleRoom::getCreatorId).distinct().collect(Collectors.toList());
            Map<Long, String> nameMap = userService.listByIds(creatorIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u.getUserName() != null ? u.getUserName() : "未知玩家"));
            rooms.forEach(r -> r.setCreatorName(nameMap.getOrDefault(r.getCreatorId(), "未知玩家")));
        }
        return rooms;
    }

    @Override
    public FishBattleRoom getActiveRoomByUserId(Long userId) {
        List<FishBattleRoom> rooms = listActiveRoomsByUserId(userId);
        return rooms.isEmpty() ? null : rooms.get(0);
    }

    @Override
    public List<FishBattleRoom> listActiveRoomsByUserId(Long userId) {
        List<FishBattleRoomPlayer> playerRecords = fishBattleRoomPlayerService.list(
                new LambdaQueryWrapper<FishBattleRoomPlayer>()
                        .eq(FishBattleRoomPlayer::getUserId, userId)
                        .eq(FishBattleRoomPlayer::getIsAi, 0));
        if (playerRecords.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roomIds = playerRecords.stream()
                .map(FishBattleRoomPlayer::getRoomId).distinct().collect(Collectors.toList());
        List<FishBattleRoom> rooms = this.list(new LambdaQueryWrapper<FishBattleRoom>()
                .in(FishBattleRoom::getId, roomIds)
                .in(FishBattleRoom::getStatus, 1, 2)
                .orderByDesc(FishBattleRoom::getCreateTime));
        return rooms;
    }

    @Override
    public void updatePlayersHeroSelection(Long dbRoomId, List<Map<String, Object>> playerHeroData) {
        for (Map<String, Object> data : playerHeroData) {
            Long userId = (Long) data.get("userId");
            FishBattleRoomPlayer record = fishBattleRoomPlayerService.getOne(
                    new LambdaQueryWrapper<FishBattleRoomPlayer>()
                            .eq(FishBattleRoomPlayer::getRoomId, dbRoomId)
                            .eq(FishBattleRoomPlayer::getUserId, userId));
            if (record != null) {
                record.setHeroId((String) data.get("heroId"));
                record.setSkinId((String) data.get("skinId"));
                record.setSpell1((String) data.get("spell1"));
                record.setSpell2((String) data.get("spell2"));
                fishBattleRoomPlayerService.updateById(record);
            }
        }
    }

    @Override
    public void updateRoomStatus(Long dbRoomId, int status) {
        FishBattleRoom room = new FishBattleRoom();
        room.setId(dbRoomId);
        room.setStatus(status);
        this.updateById(room);
    }

    @Override
    public int cleanupStaleRooms() {
        long count = this.count(new LambdaQueryWrapper<FishBattleRoom>()
                .in(FishBattleRoom::getStatus, 1, 2));
        if (count == 0) {
            return 0;
        }
        this.update(new LambdaUpdateWrapper<FishBattleRoom>()
                .in(FishBattleRoom::getStatus, 1, 2)
                .set(FishBattleRoom::getStatus, 3));
        return (int) count;
    }

    /**
     * 生成6位随机房间编码
     */
    private String generateRoomCode() {
        return RandomUtil.randomString("ABCDEFGHJKLMNPQRSTUVWXYZ23456789", 6);
    }
}
