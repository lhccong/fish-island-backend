package com.cong.fishisland.service.impl;

import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.UndercoverGameRedisKey;
import com.cong.fishisland.model.entity.game.UndercoverRoom;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.enums.MessageTypeEnum;
import com.cong.fishisland.model.enums.RoomStatusEnum;
import com.cong.fishisland.model.ws.request.MessageWrapper;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import com.cong.fishisland.service.AsyncGameService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.websocket.service.WebSocketService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 异步游戏服务实现
 *
 * @author cong
 */
@Service
@Slf4j
public class AsyncGameServiceImpl implements AsyncGameService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private UserService userService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private WebSocketService webSocketService;
    

    /**
     * 按房间存活玩家顺序依次发送发言提醒，发送间隔20秒，全部玩家发送完毕后提醒投票，投票时间30秒后自动结算
     *
     * @param roomId 房间ID
     */
    @Async("taskExecutor")
    @Override
    public void startSpeakingAndVoting(String roomId) {
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            log.error("房间ID不能为空");
            return;
        }

        // 获取房间信息
        String roomJson = stringRedisTemplate.opsForValue().get(
                UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId));
        if (roomJson == null) {
            log.error("房间不存在: {}", roomId);
            return;
        }

        try {
            UndercoverRoom room = objectMapper.readValue(roomJson, UndercoverRoom.class);

            // 检查房间状态
            if (room.getStatus() != RoomStatusEnum.PLAYING) {
                log.error("房间未开始游戏或已结束: {}", roomId);
                return;
            }

            // 获取所有未淘汰的玩家，按照顺序
            List<Long> activePlayers = new ArrayList<>();
            if (room.getOrderedParticipantIds() != null) {
                for (Long playerId : room.getOrderedParticipantIds()) {
                    if (!room.getEliminatedIds().contains(playerId)) {
                        activePlayers.add(playerId);
                    }
                }
            }

            if (activePlayers.isEmpty()) {
                log.error("房间内没有活跃玩家: {}", roomId);
                return;
            }

            // 直接在当前异步方法中执行逻辑
            try {
                for (Long playerId : activePlayers) {
                    User player = userService.getById(playerId);
                    
                    if (player != null) {
                        String speakingMessage = "请玩家【" + player.getUserName() + "】开始发言，描述自己拿到的词语！";
                        MessageWrapper speakingNotice = getSystemMessageWrapper(speakingMessage);
                        webSocketService.sendToAllOnline(WSBaseResp.builder()
                                .type(MessageTypeEnum.UNDERCOVER.getType())
                                .data(speakingNotice).build());
                        
                        // 单独给当前发言玩家发送提醒
                        Map<String, Object> params = new HashMap<>();
                        params.put("roomId", roomId);
                        params.put("time", 20);
                        WSBaseResp<Object> infoResp = WSBaseResp.builder()
                                .type(MessageTypeEnum.COUNTDOWN.getType())
                                .data(params)
                                .build();
                        webSocketService.sendToUid(infoResp, playerId);
                        
                        // 等待20秒
                        Thread.sleep(20000);
                    }
                }
                
                // 所有玩家发言完毕，提醒开始投票
                MessageWrapper voteStartMessage = getSystemMessageWrapper("所有玩家已发言完毕，现在开始投票环节！请在30秒内完成投票，投出你认为是卧底的玩家。");
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.UNDERCOVER.getType())
                        .data(voteStartMessage).build());
                
                // 等待30秒后自动结算
                Thread.sleep(30000);
                
                // 检查房间是否还存在
                String checkRoomJson = stringRedisTemplate.opsForValue().get(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId));
                if (checkRoomJson != null) {
                    MessageWrapper timeUpMessage = getSystemMessageWrapper("投票时间结束，即将进行结算！");
                    webSocketService.sendToAllOnline(WSBaseResp.builder()
                            .type(MessageTypeEnum.UNDERCOVER.getType())
                            .data(timeUpMessage).build());
                    
                    // 调用结算方法
                   endGame(roomId);
                }
            } catch (InterruptedException e) {
                log.error("发言投票流程被中断", e);
            } catch (Exception e) {
                log.error("发言投票流程出错", e);
            }
        } catch (JsonProcessingException e) {
            log.error("解析房间信息失败", e);
        }
    }

    public boolean endGame(String roomId) {
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
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

                // 如果游戏未开始，直接结束
                if (room.getStatus() != RoomStatusEnum.PLAYING) {

                    // 更新房间信息
                    stringRedisTemplate.delete(UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId));


                    return true;
                }

                // 如果游戏已开始，进行投票统计和游戏结果判断
                // 1. 统计投票数，找出票数最多的玩家
                Map<Long, Integer> voteCountMap = new HashMap<>();
                Long mostVotedPlayer = null;
                int maxVotes = -1;

                // 获取所有未淘汰的玩家
                Set<Long> activePlayers = new HashSet<>(room.getParticipantIds());
                activePlayers.removeAll(room.getEliminatedIds());

                // 统计每个玩家的票数
                for (Long playerId : activePlayers) {
                    String voteCountStr = stringRedisTemplate.opsForValue().get(
                            UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_VOTE_COUNT, roomId) + ":" + playerId);
                    int voteCount = 0;
                    if (voteCountStr != null) {
                        voteCount = Integer.parseInt(voteCountStr);
                    }
                    voteCountMap.put(playerId, voteCount);

                    // 更新最高票数玩家
                    if (voteCount > maxVotes) {
                        maxVotes = voteCount;
                        mostVotedPlayer = playerId;
                    }
                }

                // 2. 如果有投票，处理投票结果
                boolean shouldEndGame = false;
                String gameResult = "";
                if (maxVotes == 0) {
                    shouldEndGame = true;
                    gameResult = "暂无人投票，游戏结束参与积分已退回";
                }

                if (mostVotedPlayer != null && maxVotes > 0) {
                    // 判断最高票数的玩家是否为卧底
                    boolean isUndercover = room.getUndercoverIds().contains(mostVotedPlayer);

                    // 获取被淘汰玩家信息
                    User eliminatedUser = userService.getById(mostVotedPlayer);
                    String eliminatedUserName = eliminatedUser != null ? eliminatedUser.getUserName() : "未知玩家";

                    // 淘汰投票最多的玩家
                    room.getEliminatedIds().add(mostVotedPlayer);

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
                    if (remainingUndercovers == 0) {
                        // 所有卧底被淘汰，平民获胜
                        shouldEndGame = true;

                        // 获取所有卧底的名字
                        StringBuilder undercoverNames = new StringBuilder();
                        for (Long undercoverId : room.getUndercoverIds()) {
                            User undercoverUser = userService.getById(undercoverId);
                            if (undercoverUser != null) {
                                if (undercoverNames.length() > 0) {
                                    undercoverNames.append("、");
                                }
                                undercoverNames.append(undercoverUser.getUserName());
                            }
                        }

                        gameResult = "平民获胜！所有卧底已被淘汰！卧底是：" + undercoverNames
                                + "。平民词语是【" + room.getCivilianWord() + "】，卧底词语是【" + (room.getGameMode() == 2 ? "🈚️" : room.getUndercoverWord()) + "】";
                    } else if (remainingUndercovers >= remainingCivilians) {
                        // 卧底人数大于等于平民人数，卧底获胜
                        shouldEndGame = true;

                        // 获取所有卧底的名字
                        StringBuilder undercoverNames = new StringBuilder();
                        for (Long undercoverId : room.getUndercoverIds()) {
                            User undercoverUser = userService.getById(undercoverId);
                            if (undercoverUser != null) {
                                if (undercoverNames.length() > 0) {
                                    undercoverNames.append("、");
                                }
                                undercoverNames.append(undercoverUser.getUserName());
                            }
                        }

                        gameResult = "卧底获胜！卧底人数已超过或等于平民人数！卧底是：" + undercoverNames
                                + "。平民词语是【" + room.getCivilianWord() + "】，卧底词语是【" + (room.getGameMode() == 2 ? "🈚️" : room.getUndercoverWord()) + "】";
                    } else {
                        // 游戏继续，显示谁被淘汰了
                        if (isUndercover) {
                            gameResult = "玩家【" + eliminatedUserName + "】被淘汰，他是卧底！。还有" + remainingUndercovers + "名卧底未被发现，游戏继续...";
                        } else {
                            gameResult = "玩家【" + eliminatedUserName + "】被淘汰，他是平民！剩余平民" + remainingCivilians + "人，卧底" + remainingUndercovers + "人，游戏继续...";
                        }

                        // 保存淘汰信息但不结束游戏
                        stringRedisTemplate.opsForValue().set(
                                UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_RESULT, roomId),
                                gameResult,
                                60,
                                TimeUnit.MINUTES
                        );
                        // 调用异步服务的方法
                        startSpeakingAndVoting(roomId);
                    }
                }

                // 3. 更新游戏状态
                if (shouldEndGame) {
                    room.setStatus(RoomStatusEnum.ENDED);
                    // 将游戏结果保存到 Redis，可以添加一个新的键
                    stringRedisTemplate.opsForValue().set(
                            UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_RESULT, roomId),
                            gameResult,
                            60,
                            TimeUnit.MINUTES
                    );
                    // 更新房间信息
                    String updatedRoomJson = objectMapper.writeValueAsString(room);
                    stringRedisTemplate.opsForValue().set(
                            UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId),
                            updatedRoomJson,
                            1,
                            TimeUnit.MINUTES
                    );
                    // 清除所有玩家的角色信息
                    for (Long playerId : room.getParticipantIds()) {
                        // 删除玩家角色信息
                        stringRedisTemplate.delete(
                                UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_ROLE, playerId)
                        );
                        // 删除玩家所在房间信息
                        stringRedisTemplate.delete(
                                UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_ROOM, playerId)
                        );
                        // 删除玩家的投票状态
                        stringRedisTemplate.delete(
                                UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_VOTED, roomId, playerId)
                        );
                        // 删除玩家收到的投票计数
                        stringRedisTemplate.delete(
                                UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_VOTE_COUNT, roomId) + ":" + playerId
                        );

                        // 删除玩家猜词次数记录
                        stringRedisTemplate.delete(
                                UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_GUESS_COUNT, roomId, playerId)
                        );
                    }

                    // 删除房间的投票记录
                    stringRedisTemplate.delete(
                            UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_VOTES, roomId)
                    );
                } else {
                    // 如果游戏继续，确保状态为 PLAYING
                    room.setStatus(RoomStatusEnum.PLAYING);

                    // 如果有投票记录，需要清除所有玩家的投票状态，以便下一轮投票
                    if (mostVotedPlayer != null) {
                        // 清除所有玩家的投票状态
                        for (Long playerId : room.getParticipantIds()) {
                            stringRedisTemplate.delete(
                                    UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_VOTED, roomId, playerId)
                            );
                        }

                        // 清除投票计数
                        for (Long playerId : room.getParticipantIds()) {
                            stringRedisTemplate.delete(
                                    UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_VOTE_COUNT, roomId) + ":" + playerId
                            );
                        }

                        // 清除投票记录
                        stringRedisTemplate.delete(
                                UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_VOTES, roomId)
                        );
                    }
                    // 更新房间信息
                    String updatedRoomJson = objectMapper.writeValueAsString(room);
                    stringRedisTemplate.opsForValue().set(
                            UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId),
                            updatedRoomJson,
                            60,
                            TimeUnit.MINUTES
                    );
                }


                //发送消息给每个人
                MessageWrapper messageWrapper = getSystemMessageWrapper(gameResult);
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.UNDERCOVER.getType())
                        .data(messageWrapper).build());

                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.REFRESH_ROOM.getType())
                        .data("").build());
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
    
    /**
     * 获取系统消息包装器
     */
    private MessageWrapper getSystemMessageWrapper(String content) {
        com.cong.fishisland.model.ws.request.Message message = new com.cong.fishisland.model.ws.request.Message();
        message.setId("-1");
        message.setContent(content);
        com.cong.fishisland.model.ws.request.Sender sender = new com.cong.fishisland.model.ws.request.Sender();
        sender.setId("-1");
        sender.setName("摸鱼小助手");
        sender.setAvatar("https://s1.aigei.com/src/img/gif/41/411d8d587bfc41aeaadfb44ae246da0d.gif?imageMogr2/auto-orient/thumbnail/!282x282r/gravity/Center/crop/282x282/quality/85/%7CimageView2/2/w/282&e=2051020800&token=P7S2Xpzfz11vAkASLTkfHN7Fw-oOZBecqeJaxypL:OU5w-4wX8swq04CJ3p4N0tl_J7E=");
        sender.setPoints(0);
        sender.setLevel(1);
        sender.setUserProfile("");
        sender.setAvatarFramerUrl("");
        sender.setTitleId(null);
        sender.setTitleIdList(null);
        sender.setRegion("摸鱼岛");
        sender.setCountry("摸鱼～");

        message.setSender(sender);
        message.setTimestamp(java.time.Instant.now().toString());

        MessageWrapper messageWrapper = new MessageWrapper();
        messageWrapper.setMessage(message);
        return messageWrapper;
    }
} 