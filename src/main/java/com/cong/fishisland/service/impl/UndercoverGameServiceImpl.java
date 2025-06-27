package com.cong.fishisland.service.impl;

import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.UndercoverGameRedisKey;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.game.UndercoverRoomCreateRequest;
import com.cong.fishisland.model.entity.game.UndercoverRoom;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.game.UndercoverPlayerVO;
import com.cong.fishisland.model.vo.game.UndercoverRoomVO;
import com.cong.fishisland.service.UndercoverGameService;
import com.cong.fishisland.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 谁是卧底游戏服务实现
 *
 * @author cong
 */
@Service
@Slf4j
public class UndercoverGameServiceImpl implements UndercoverGameService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private UserService userService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public String createRoom(UndercoverRoomCreateRequest request) {
        // 验证请求参数
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        if (StringUtils.isBlank(request.getCivilianWord())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "平民词语不能为空");
        }
        if (StringUtils.isBlank(request.getUndercoverWord())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "卧底词语不能为空");
        }
        if (request.getDuration() == null || request.getDuration() < 60) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "游戏持续时间不能少于60秒");
        }
        
        // 验证是否为管理员
        User loginUser = userService.getLoginUser();
        if (!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有管理员可以创建房间");
        }

        // 使用分布式锁确保同一时间只能有一个房间
        RLock lock = redissonClient.getLock("undercover_room_create_lock");
        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后再试");
            }

            // 检查是否已有活跃房间
            String activeRoomId = stringRedisTemplate.opsForValue().get(UndercoverGameRedisKey.ACTIVE_ROOM);
            if (activeRoomId != null) {
                // 获取房间信息
                String roomJson = stringRedisTemplate.opsForValue().get(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, activeRoomId));
                if (roomJson != null) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "已存在活跃房间，请等待当前房间结束");
                }
            }

            // 创建新房间
            UndercoverRoom room = new UndercoverRoom();
            room.setStatus(UndercoverRoom.RoomStatus.WAITING);
            room.setParticipantIds(new HashSet<>());
            room.setUndercoverIds(new HashSet<>());
            room.setCivilianIds(new HashSet<>());
            room.setEliminatedIds(new HashSet<>());
            room.setCivilianWord(request.getCivilianWord());
            room.setUndercoverWord(request.getUndercoverWord());
            room.setCreateTime(new Date());
            room.setDuration(request.getDuration());
            room.setCreatorId(loginUser.getId());

            // 生成房间ID
            String roomId = UUID.randomUUID().toString().replace("-", "");

            // 存储房间信息到Redis
            try {
                String roomJson = objectMapper.writeValueAsString(room);
                stringRedisTemplate.opsForValue().set(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId),
                        roomJson,
                        24,
                        TimeUnit.MINUTES
                );
                // 设置当前活跃房间
                stringRedisTemplate.opsForValue().set(UndercoverGameRedisKey.ACTIVE_ROOM, roomId, 24, TimeUnit.HOURS);
                return roomId;
            } catch (JsonProcessingException e) {
                log.error("序列化房间信息失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建房间失败");
            }
        } catch (InterruptedException e) {
            log.error("获取锁失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后再试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public UndercoverRoomVO getActiveRoom() {
        // 获取当前活跃房间ID
        String roomId = stringRedisTemplate.opsForValue().get(UndercoverGameRedisKey.ACTIVE_ROOM);
        if (roomId == null) {
            return null;
        }

        // 获取房间信息
        String roomJson = stringRedisTemplate.opsForValue().get(
                UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId));
        if (roomJson == null) {
            // 如果房间不存在，清除活跃房间记录
            stringRedisTemplate.delete(UndercoverGameRedisKey.ACTIVE_ROOM);
            return null;
        }

        try {
            UndercoverRoom room = objectMapper.readValue(roomJson, UndercoverRoom.class);
            UndercoverRoomVO roomVO = new UndercoverRoomVO();
            BeanUtils.copyProperties(room, roomVO);
            roomVO.setRoomId(roomId);

            // 计算剩余时间
            if (room.getStartTime() != null && room.getDuration() != null) {
                long elapsedTime = (System.currentTimeMillis() - room.getStartTime().getTime()) / 1000;
                int remainingTime = (int) Math.max(0, room.getDuration() - elapsedTime);
                roomVO.setRemainingTime(remainingTime);
                
                // 如果时间到了但游戏还在进行中，自动结束游戏
                if (remainingTime <= 0 && room.getStatus() == UndercoverRoom.RoomStatus.PLAYING) {
                    endGame(roomId);
                    roomVO.setStatus(UndercoverRoom.RoomStatus.ENDED);
                }
            }

            return roomVO;
        } catch (JsonProcessingException e) {
            log.error("解析房间信息失败", e);
            return null;
        }
    }

    @Override
    public boolean joinRoom(String roomId) {
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        
        // 验证用户登录状态
        User loginUser = userService.getLoginUser();
        
        // 使用分布式锁确保并发安全
        RLock lock = redissonClient.getLock("undercover_room_join_lock:" + roomId);
        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后再试");
            }

            // 获取房间信息
            String roomJson = stringRedisTemplate.opsForValue().get(
                    UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId));
            if (roomJson == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "房间不存在");
            }

            try {
                UndercoverRoom room = objectMapper.readValue(roomJson, UndercoverRoom.class);
                
                // 检查房间状态
                if (room.getStatus() != UndercoverRoom.RoomStatus.WAITING) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "房间已开始游戏或已结束，无法加入");
                }
                
                // 检查用户是否已在房间中
                if (room.getParticipantIds().contains(loginUser.getId())) {
                    return true;
                }
                
                // 将用户添加到房间
                room.getParticipantIds().add(loginUser.getId());
                
                // 更新房间信息
                String updatedRoomJson = objectMapper.writeValueAsString(room);
                stringRedisTemplate.opsForValue().set(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId),
                        updatedRoomJson,
                        24,
                        TimeUnit.HOURS
                );
                
                // 记录用户所在房间
                stringRedisTemplate.opsForValue().set(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_ROOM, loginUser.getId()),
                        roomId,
                        24,
                        TimeUnit.HOURS
                );
                
                return true;
            } catch (JsonProcessingException e) {
                log.error("解析房间信息失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加入房间失败");
            }
        } catch (InterruptedException e) {
            log.error("获取锁失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后再试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public boolean startGame(String roomId) {
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        
        // 验证是否为管理员
        User loginUser = userService.getLoginUser();
        if (!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有管理员可以开始游戏");
        }
        
        // 使用分布式锁确保并发安全
        RLock lock = redissonClient.getLock("undercover_room_start_lock:" + roomId);
        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后再试");
            }

            // 获取房间信息
            String roomJson = stringRedisTemplate.opsForValue().get(
                    UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId));
            if (roomJson == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "房间不存在");
            }

            try {
                UndercoverRoom room = objectMapper.readValue(roomJson, UndercoverRoom.class);
                
                // 检查房间状态
                if (room.getStatus() != UndercoverRoom.RoomStatus.WAITING) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "房间已开始或已结束");
                }
                
                // 检查参与者数量
                if (room.getParticipantIds().size() < 3) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "参与者数量不足，至少需要3人");
                }
                
                // 分配角色
                assignRoles(room);
                
                // 更新房间状态
                room.setStatus(UndercoverRoom.RoomStatus.PLAYING);
                room.setStartTime(new Date());
                
                // 更新房间信息
                String updatedRoomJson = objectMapper.writeValueAsString(room);
                stringRedisTemplate.opsForValue().set(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId),
                        updatedRoomJson,
                        24,
                        TimeUnit.HOURS
                );
                
                return true;
            } catch (JsonProcessingException e) {
                log.error("解析房间信息失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "开始游戏失败");
            }
        } catch (InterruptedException e) {
            log.error("获取锁失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后再试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 分配角色
     *
     * @param room 房间信息
     */
    private void assignRoles(UndercoverRoom room) {
        List<Long> participants = new ArrayList<>(room.getParticipantIds());
        Collections.shuffle(participants);
        
        // 确定卧底数量（约1/3的玩家，至少1人）
        int undercoverCount = Math.max(1, participants.size() / 3);
        
        // 清空现有角色分配
        room.getUndercoverIds().clear();
        room.getCivilianIds().clear();
        
        // 分配角色
        for (int i = 0; i < participants.size(); i++) {
            Long userId = participants.get(i);
            if (i < undercoverCount) {
                room.getUndercoverIds().add(userId);
                // 存储玩家角色信息
                stringRedisTemplate.opsForValue().set(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_ROLE, userId),
                        "undercover",
                        24,
                        TimeUnit.HOURS
                );
            } else {
                room.getCivilianIds().add(userId);
                // 存储玩家角色信息
                stringRedisTemplate.opsForValue().set(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_ROLE, userId),
                        "civilian",
                        24,
                        TimeUnit.HOURS
                );
            }
        }
    }

    @Override
    public boolean endGame(String roomId) {
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        
        // 验证是否为管理员
        User loginUser = userService.getLoginUser();
        if (!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有管理员可以结束游戏");
        }
        
        // 使用分布式锁确保并发安全
        RLock lock = redissonClient.getLock("undercover_room_end_lock:" + roomId);
        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后再试");
            }

            // 获取房间信息
            String roomJson = stringRedisTemplate.opsForValue().get(
                    UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId));
            if (roomJson == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "房间不存在");
            }

            try {
                UndercoverRoom room = objectMapper.readValue(roomJson, UndercoverRoom.class);
                
                // 更新房间状态
                room.setStatus(UndercoverRoom.RoomStatus.ENDED);
                
                // 更新房间信息
                String updatedRoomJson = objectMapper.writeValueAsString(room);
                stringRedisTemplate.opsForValue().set(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId),
                        updatedRoomJson,
                        24,
                        TimeUnit.MINUTES
                );
                
                // 清除活跃房间
                String activeRoomId = stringRedisTemplate.opsForValue().get(UndercoverGameRedisKey.ACTIVE_ROOM);
                if (roomId.equals(activeRoomId)) {
                    stringRedisTemplate.delete(UndercoverGameRedisKey.ACTIVE_ROOM);
                }
                
                return true;
            } catch (JsonProcessingException e) {
                log.error("解析房间信息失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "结束游戏失败");
            }
        } catch (InterruptedException e) {
            log.error("获取锁失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后再试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public UndercoverPlayerVO getPlayerInfo(String roomId, Long userId) {
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }
        
        // 验证用户登录状态
        User currentUser = userService.getLoginUser();
        
        // 如果不是查询自己且不是管理员，则无权限
        if (!currentUser.getId().equals(userId) && !UserConstant.ADMIN_ROLE.equals(currentUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看其他玩家信息");
        }
        
        // 获取房间信息
        String roomJson = stringRedisTemplate.opsForValue().get(
                UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId));
        if (roomJson == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "房间不存在");
        }
        
        try {
            UndercoverRoom room = objectMapper.readValue(roomJson, UndercoverRoom.class);
            
            // 检查用户是否在房间中
            if (!room.getParticipantIds().contains(userId)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户不在房间中");
            }
            
            UndercoverPlayerVO playerVO = new UndercoverPlayerVO();
            playerVO.setUserId(userId);
            
            // 获取玩家角色
            String role = stringRedisTemplate.opsForValue().get(
                    UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_ROLE, userId));
            playerVO.setRole(role);
            
            // 设置词语
            if ("undercover".equals(role)) {
                playerVO.setWord(room.getUndercoverWord());
            } else {
                playerVO.setWord(room.getCivilianWord());
            }
            
            // 设置是否被淘汰
            playerVO.setIsEliminated(room.getEliminatedIds().contains(userId));
            
            return playerVO;
        } catch (JsonProcessingException e) {
            log.error("解析房间信息失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取玩家信息失败");
        }
    }

    @Override
    public boolean eliminatePlayer(String roomId, Long userId) {
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }
        
        // 验证用户登录状态
        User loginUser = userService.getLoginUser();
        
        // 使用分布式锁确保并发安全
        RLock lock = redissonClient.getLock("undercover_room_eliminate_lock:" + roomId);
        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后再试");
            }

            // 获取房间信息
            String roomJson = stringRedisTemplate.opsForValue().get(
                    UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId));
            if (roomJson == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "房间不存在");
            }

            try {
                UndercoverRoom room = objectMapper.readValue(roomJson, UndercoverRoom.class);
                
                // 检查房间状态
                if (room.getStatus() != UndercoverRoom.RoomStatus.PLAYING) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "房间未开始游戏或已结束");
                }
                
                // 检查用户是否在房间中
                if (!room.getParticipantIds().contains(userId)) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户不在房间中");
                }
                
                // 检查用户是否已被淘汰
                if (room.getEliminatedIds().contains(userId)) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户已被淘汰");
                }
                
                // 淘汰用户
                room.getEliminatedIds().add(userId);
                
                // 更新房间信息
                String updatedRoomJson = objectMapper.writeValueAsString(room);
                stringRedisTemplate.opsForValue().set(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId),
                        updatedRoomJson,
                        24,
                        TimeUnit.HOURS
                );
                
                // 检查游戏是否结束
                boolean isGameOver = checkGameOver(roomId);
                if (isGameOver) {
                    room.setStatus(UndercoverRoom.RoomStatus.ENDED);
                    updatedRoomJson = objectMapper.writeValueAsString(room);
                    stringRedisTemplate.opsForValue().set(
                            UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId),
                            updatedRoomJson,
                            24,
                            TimeUnit.HOURS
                    );
                }
                
                return true;
            } catch (JsonProcessingException e) {
                log.error("解析房间信息失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "淘汰玩家失败");
            }
        } catch (InterruptedException e) {
            log.error("获取锁失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后再试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public boolean checkGameOver(String roomId) {
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        
        // 获取房间信息
        String roomJson = stringRedisTemplate.opsForValue().get(
                UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId));
        if (roomJson == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "房间不存在");
        }
        
        try {
            UndercoverRoom room = objectMapper.readValue(roomJson, UndercoverRoom.class);
            
            // 检查房间状态
            if (room.getStatus() != UndercoverRoom.RoomStatus.PLAYING) {
                return false;
            }
            
            // 计算剩余卧底和平民数量
            int remainingUndercovers = 0;
            int remainingCivilians = 0;
            
            for (Long userId : room.getUndercoverIds()) {
                if (!room.getEliminatedIds().contains(userId)) {
                    remainingUndercovers++;
                }
            }
            
            for (Long userId : room.getCivilianIds()) {
                if (!room.getEliminatedIds().contains(userId)) {
                    remainingCivilians++;
                }
            }
            
            // 判断游戏是否结束
            return remainingUndercovers == 0 || remainingUndercovers >= remainingCivilians;
        } catch (JsonProcessingException e) {
            log.error("解析房间信息失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "检查游戏状态失败");
        }
    }
} 