package com.cong.fishisland.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.UndercoverGameRedisKey;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.game.UndercoverRoomCreateRequest;
import com.cong.fishisland.model.dto.game.UndercoverVoteRequest;
import com.cong.fishisland.model.entity.game.UndercoverRoom;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.enums.RoomStatusEnum;
import com.cong.fishisland.model.vo.game.UndercoverPlayerDetailVO;
import com.cong.fishisland.model.vo.game.UndercoverPlayerVO;
import com.cong.fishisland.model.vo.game.UndercoverRoomVO;
import com.cong.fishisland.model.vo.game.UndercoverVoteVO;
import com.cong.fishisland.service.UndercoverGameService;
import com.cong.fishisland.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
        // 验证最大人数
        if (request.getMaxPlayers() == null || request.getMaxPlayers() < 3) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间最大人数不能少于3人");
        }
        if (request.getMaxPlayers() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间最大人数不能超过20人");
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
            room.setStatus(RoomStatusEnum.WAITING);
            room.setParticipantIds(new HashSet<>());
            room.setUndercoverIds(new HashSet<>());
            room.setCivilianIds(new HashSet<>());
            room.setEliminatedIds(new HashSet<>());
            room.setCivilianWord(request.getCivilianWord());
            room.setUndercoverWord(request.getUndercoverWord());
            room.setCreateTime(new Date());
            room.setDuration(request.getDuration());
            room.setCreatorId(loginUser.getId());
            room.setMaxPlayers(request.getMaxPlayers());

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
                if (remainingTime <= 0 && room.getStatus() == RoomStatusEnum.PLAYING) {
                    endGame(roomId);
                    roomVO.setStatus(RoomStatusEnum.ENDED);
                }
            }

            // 获取房间内所有玩家详细信息
            List<UndercoverPlayerDetailVO> participants = getRoomPlayersDetail(roomId);
            roomVO.setParticipants(participants);

            // 获取房间投票记录
            List<UndercoverVoteVO> votes = getRoomVotes(roomId);
            roomVO.setVotes(votes);

            if (StpUtil.isLogin()) {
                User currentUser = userService.getLoginUser();
                // 获取玩家角色
                String role = stringRedisTemplate.opsForValue().get(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_ROLE, currentUser.getId()));
                // 设置词语
                if ("undercover".equals(role)) {
                    roomVO.setWord(room.getUndercoverWord());
                } else if ("civilian".equals(role)) {
                    roomVO.setWord(room.getCivilianWord());
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
        //暂时无需 ID 自动获取
        roomId = stringRedisTemplate.opsForValue().get(UndercoverGameRedisKey.ACTIVE_ROOM);
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
                if (room.getStatus() != RoomStatusEnum.WAITING) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "房间已开始游戏或已结束，无法加入");
                }

                // 检查用户是否已在房间中
                if (room.getParticipantIds().contains(loginUser.getId())) {
                    return true;
                }
                
                // 检查房间是否已满
                if (room.getMaxPlayers() != null && room.getParticipantIds().size() >= room.getMaxPlayers()) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "房间已满，无法加入");
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
        //暂时无需 ID 自动获取
        roomId = stringRedisTemplate.opsForValue().get(UndercoverGameRedisKey.ACTIVE_ROOM);
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
                if (room.getStatus() != RoomStatusEnum.WAITING) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "房间已开始或已结束");
                }

                // 检查参与者数量
                if (room.getParticipantIds().size() < 3) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "参与者数量不足，至少需要3人");
                }

                // 分配角色
                assignRoles(room);

                // 更新房间状态
                room.setStatus(RoomStatusEnum.PLAYING);
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
        //暂时无需 ID 自动获取
        roomId = stringRedisTemplate.opsForValue().get(UndercoverGameRedisKey.ACTIVE_ROOM);
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
                room.setStatus(RoomStatusEnum.ENDED);

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
        //暂时无需 ID 自动获取
        roomId = stringRedisTemplate.opsForValue().get(UndercoverGameRedisKey.ACTIVE_ROOM);
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
            if (StringUtils.isBlank(role)) {
                playerVO.setWord("");
            } else if ("undercover".equals(role)) {
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
        //暂时无需 ID 自动获取
        roomId = stringRedisTemplate.opsForValue().get(UndercoverGameRedisKey.ACTIVE_ROOM);
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }

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
                if (room.getStatus() != RoomStatusEnum.PLAYING) {
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
                    room.setStatus(RoomStatusEnum.ENDED);
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
        //暂时无需 ID 自动获取
        roomId = stringRedisTemplate.opsForValue().get(UndercoverGameRedisKey.ACTIVE_ROOM);
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
            if (room.getStatus() != RoomStatusEnum.PLAYING) {
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

    @Override
    public List<UndercoverVoteVO> getRoomVotes(String roomId) {
        //暂时无需 ID 自动获取
        roomId = stringRedisTemplate.opsForValue().get(UndercoverGameRedisKey.ACTIVE_ROOM);
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }

        // 获取房间投票记录
        String votesJson = stringRedisTemplate.opsForValue().get(
                UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_VOTES, roomId));
        if (votesJson == null) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(votesJson, new TypeReference<List<UndercoverVoteVO>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("解析房间投票记录失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取房间投票记录失败");
        }
    }

    @Override
    public boolean vote(UndercoverVoteRequest request) {
        // 验证参数
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "投票请求不能为空");
        }
        String roomId;
        //暂时无需 ID 自动获取
        roomId = stringRedisTemplate.opsForValue().get(UndercoverGameRedisKey.ACTIVE_ROOM);
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        Long targetId = request.getTargetId();
        if (targetId == null || targetId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "投票目标不合法");
        }

        // 验证用户登录状态
        User loginUser = userService.getLoginUser();

        // 使用分布式锁确保并发安全
        RLock lock = redissonClient.getLock("undercover_room_vote_lock:" + roomId);
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
                if (room.getStatus() != RoomStatusEnum.PLAYING) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "房间未开始游戏或已结束");
                }

                // 检查用户是否在房间中
                if (!room.getParticipantIds().contains(loginUser.getId())) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户不在房间中");
                }

                // 检查用户是否已被淘汰
                if (room.getEliminatedIds().contains(loginUser.getId())) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户已被淘汰");
                }

                // 检查投票目标是否在房间中
                if (!room.getParticipantIds().contains(targetId)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "投票目标不在房间中");
                }

                // 检查投票目标是否已被淘汰
                if (room.getEliminatedIds().contains(targetId)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "投票目标已被淘汰");
                }

                // 检查是否已投票
                String hasVoted = stringRedisTemplate.opsForValue().get(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_VOTED, roomId, loginUser.getId()));
                if (hasVoted != null) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户已投票");
                }

                // 获取投票者和目标用户信息
                User voter = userService.getById(loginUser.getId());
                User target = userService.getById(targetId);

                // 创建投票记录
                UndercoverVoteVO voteVO = new UndercoverVoteVO();
                voteVO.setVoterId(loginUser.getId());
                voteVO.setVoterName(voter.getUserName());
                voteVO.setVoterAvatar(voter.getUserAvatar());
                voteVO.setTargetId(targetId);
                voteVO.setTargetName(target.getUserName());
                voteVO.setTargetAvatar(target.getUserAvatar());
                voteVO.setVoteTime(new Date());

                // 获取当前投票记录
                List<UndercoverVoteVO> votes = new ArrayList<>();
                String votesJson = stringRedisTemplate.opsForValue().get(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_VOTES, roomId));
                if (votesJson != null) {
                    votes = objectMapper.readValue(votesJson, new TypeReference<List<UndercoverVoteVO>>() {
                    });
                }

                // 添加新投票记录
                votes.add(voteVO);

                // 更新投票记录
                String updatedVotesJson = objectMapper.writeValueAsString(votes);
                stringRedisTemplate.opsForValue().set(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_VOTES, roomId),
                        updatedVotesJson,
                        24,
                        TimeUnit.HOURS
                );

                // 更新投票计数
                String voteCountKey = UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_VOTE_COUNT, roomId) + ":" + targetId;
                String voteCountStr = stringRedisTemplate.opsForValue().get(voteCountKey);
                int voteCount = 1;
                if (voteCountStr != null) {
                    voteCount = Integer.parseInt(voteCountStr) + 1;
                }
                stringRedisTemplate.opsForValue().set(voteCountKey, String.valueOf(voteCount), 24, TimeUnit.HOURS);

                // 标记用户已投票
                stringRedisTemplate.opsForValue().set(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_VOTED, roomId, loginUser.getId()),
                        "1",
                        24,
                        TimeUnit.HOURS
                );

                return true;
            } catch (JsonProcessingException e) {
                log.error("处理投票失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "投票失败");
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
    public UndercoverPlayerDetailVO getPlayerDetailInfo(String roomId, Long userId) {
        //暂时无需 ID 自动获取
        roomId = stringRedisTemplate.opsForValue().get(UndercoverGameRedisKey.ACTIVE_ROOM);
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
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

            // 获取用户信息
            User user = userService.getById(userId);
            if (user == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            }

            UndercoverPlayerDetailVO playerDetailVO = new UndercoverPlayerDetailVO();
            playerDetailVO.setUserId(userId);
            playerDetailVO.setUserName(user.getUserName());
            playerDetailVO.setUserAvatar(user.getUserAvatar());


            // 设置是否被淘汰
            playerDetailVO.setIsEliminated(room.getEliminatedIds().contains(userId));

            // 获取玩家收到的票数
            String voteCountStr = stringRedisTemplate.opsForValue().get(
                    UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_VOTE_COUNT, roomId) + ":" + userId);
            int voteCount = 0;
            if (voteCountStr != null) {
                voteCount = Integer.parseInt(voteCountStr);
            }
            playerDetailVO.setVoteCount(voteCount);

            return playerDetailVO;
        } catch (JsonProcessingException e) {
            log.error("解析房间信息失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取玩家信息失败");
        }
    }

    @Override
    public List<UndercoverPlayerDetailVO> getRoomPlayersDetail(String roomId) {
        //暂时无需 ID 自动获取
        roomId = stringRedisTemplate.opsForValue().get(UndercoverGameRedisKey.ACTIVE_ROOM);
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
            List<UndercoverPlayerDetailVO> playerDetails = new ArrayList<>();

            // 获取所有参与者的详细信息
            for (Long userId : room.getParticipantIds()) {
                User user = userService.getById(userId);
                if (user == null) {
                    continue;
                }

                UndercoverPlayerDetailVO playerDetailVO = new UndercoverPlayerDetailVO();
                playerDetailVO.setUserId(userId);
                playerDetailVO.setUserName(user.getUserName());
                playerDetailVO.setUserAvatar(user.getUserAvatar());

                // 设置是否被淘汰
                playerDetailVO.setIsEliminated(room.getEliminatedIds().contains(userId));

                // 获取玩家收到的票数
                String voteCountStr = stringRedisTemplate.opsForValue().get(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_VOTE_COUNT, roomId) + ":" + userId);
                int voteCount = 0;
                if (voteCountStr != null) {
                    voteCount = Integer.parseInt(voteCountStr);
                }
                playerDetailVO.setVoteCount(voteCount);

                playerDetails.add(playerDetailVO);
            }

            return playerDetails;
        } catch (JsonProcessingException e) {
            log.error("解析房间信息失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取房间玩家信息失败");
        }
    }
} 