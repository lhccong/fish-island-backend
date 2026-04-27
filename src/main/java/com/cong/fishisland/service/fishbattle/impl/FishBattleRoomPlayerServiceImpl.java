package com.cong.fishisland.service.fishbattle.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.fishbattle.FishBattleRoomPlayerMapper;
import com.cong.fishisland.model.entity.fishbattle.FishBattleRoomPlayer;
import com.cong.fishisland.service.fishbattle.FishBattleRoomPlayerService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 摸鱼大乱斗房间玩家服务实现
 */
@Service
public class FishBattleRoomPlayerServiceImpl extends ServiceImpl<FishBattleRoomPlayerMapper, FishBattleRoomPlayer>
        implements FishBattleRoomPlayerService {

    @Override
    public List<FishBattleRoomPlayer> listByRoomId(Long roomId) {
        return this.list(new LambdaQueryWrapper<FishBattleRoomPlayer>()
                .eq(FishBattleRoomPlayer::getRoomId, roomId)
                .orderByAsc(FishBattleRoomPlayer::getTeam)
                .orderByAsc(FishBattleRoomPlayer::getSlotIndex));
    }

    @Override
    public FishBattleRoomPlayer joinRoom(Long roomId, Long userId, String team) {
        long teamCount = this.count(new LambdaQueryWrapper<FishBattleRoomPlayer>()
                .eq(FishBattleRoomPlayer::getRoomId, roomId)
                .eq(FishBattleRoomPlayer::getTeam, team));

        FishBattleRoomPlayer player = new FishBattleRoomPlayer();
        player.setRoomId(roomId);
        player.setUserId(userId);
        player.setTeam(team);
        player.setIsReady(0);
        player.setIsAi(0);
        player.setSlotIndex((int) teamCount);
        this.save(player);
        return player;
    }

    @Override
    public boolean leaveRoom(Long roomId, Long userId) {
        return this.remove(new LambdaQueryWrapper<FishBattleRoomPlayer>()
                .eq(FishBattleRoomPlayer::getRoomId, roomId)
                .eq(FishBattleRoomPlayer::getUserId, userId));
    }

    @Override
    public boolean toggleReady(Long roomId, Long userId) {
        FishBattleRoomPlayer player = this.getOne(new LambdaQueryWrapper<FishBattleRoomPlayer>()
                .eq(FishBattleRoomPlayer::getRoomId, roomId)
                .eq(FishBattleRoomPlayer::getUserId, userId));
        if (player == null) {
            return false;
        }
        player.setIsReady(player.getIsReady() == 0 ? 1 : 0);
        return this.updateById(player);
    }

    @Override
    public FishBattleRoomPlayer joinRoomWithSlot(Long roomId, Long userId, String team, int slotIndex) {
        // 检查该玩家是否已在房间中
        FishBattleRoomPlayer existing = this.getOne(new LambdaQueryWrapper<FishBattleRoomPlayer>()
                .eq(FishBattleRoomPlayer::getRoomId, roomId)
                .eq(FishBattleRoomPlayer::getUserId, userId));
        if (existing != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "你已在该房间中");
        }
        // 检查目标位置是否已被占用
        FishBattleRoomPlayer occupant = this.getOne(new LambdaQueryWrapper<FishBattleRoomPlayer>()
                .eq(FishBattleRoomPlayer::getRoomId, roomId)
                .eq(FishBattleRoomPlayer::getTeam, team)
                .eq(FishBattleRoomPlayer::getSlotIndex, slotIndex));
        if (occupant != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该位置已被占用");
        }
        // 检查队伍人数上限（每队5人）
        long teamCount = this.count(new LambdaQueryWrapper<FishBattleRoomPlayer>()
                .eq(FishBattleRoomPlayer::getRoomId, roomId)
                .eq(FishBattleRoomPlayer::getTeam, team));
        if (teamCount >= 5) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该队伍已满");
        }
        FishBattleRoomPlayer player = new FishBattleRoomPlayer();
        player.setRoomId(roomId);
        player.setUserId(userId);
        player.setTeam(team);
        player.setSlotIndex(slotIndex);
        player.setIsReady(0);
        player.setIsAi(0);
        this.save(player);
        return player;
    }

    @Override
    public boolean switchTeam(Long roomId, Long userId, String newTeam, int newSlotIndex) {
        // 查询当前玩家记录
        FishBattleRoomPlayer player = this.getOne(new LambdaQueryWrapper<FishBattleRoomPlayer>()
                .eq(FishBattleRoomPlayer::getRoomId, roomId)
                .eq(FishBattleRoomPlayer::getUserId, userId));
        if (player == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "你不在该房间中");
        }
        // 如果目标位置与当前相同，无需切换
        if (newTeam.equals(player.getTeam()) && newSlotIndex == player.getSlotIndex()) {
            return true;
        }
        // 检查目标位置是否已被占用
        FishBattleRoomPlayer occupant = this.getOne(new LambdaQueryWrapper<FishBattleRoomPlayer>()
                .eq(FishBattleRoomPlayer::getRoomId, roomId)
                .eq(FishBattleRoomPlayer::getTeam, newTeam)
                .eq(FishBattleRoomPlayer::getSlotIndex, newSlotIndex));
        if (occupant != null) {
            return false;
        }
        // 检查目标队伍人数（排除自己如果是跨队切换）
        if (!newTeam.equals(player.getTeam())) {
            long targetTeamCount = this.count(new LambdaQueryWrapper<FishBattleRoomPlayer>()
                    .eq(FishBattleRoomPlayer::getRoomId, roomId)
                    .eq(FishBattleRoomPlayer::getTeam, newTeam));
            if (targetTeamCount >= 5) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "目标队伍已满");
            }
        }
        player.setTeam(newTeam);
        player.setSlotIndex(newSlotIndex);
        player.setIsReady(0);
        return this.updateById(player);
    }

    @Override
    public long countRealPlayers(Long roomId) {
        return this.count(new LambdaQueryWrapper<FishBattleRoomPlayer>()
                .eq(FishBattleRoomPlayer::getRoomId, roomId)
                .eq(FishBattleRoomPlayer::getIsAi, 0));
    }

    @Override
    public boolean removeByRoomId(Long roomId) {
        return this.remove(new LambdaQueryWrapper<FishBattleRoomPlayer>()
                .eq(FishBattleRoomPlayer::getRoomId, roomId));
    }
}
