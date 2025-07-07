package com.cong.fishisland.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.UndercoverGameRedisKey;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.game.UndercoverGuessRequest;
import com.cong.fishisland.model.dto.game.UndercoverRoomCreateRequest;
import com.cong.fishisland.model.dto.game.UndercoverVoteRequest;
import com.cong.fishisland.model.entity.game.UndercoverRoom;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.enums.MessageTypeEnum;
import com.cong.fishisland.model.enums.RoomStatusEnum;
import com.cong.fishisland.model.vo.game.UndercoverPlayerDetailVO;
import com.cong.fishisland.model.vo.game.UndercoverPlayerVO;
import com.cong.fishisland.model.vo.game.UndercoverRoomVO;
import com.cong.fishisland.model.vo.game.UndercoverVoteVO;
import com.cong.fishisland.model.ws.request.Message;
import com.cong.fishisland.model.ws.request.MessageWrapper;
import com.cong.fishisland.model.ws.request.Sender;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import com.cong.fishisland.service.AsyncGameService;
import com.cong.fishisland.service.UndercoverGameService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.websocket.service.WebSocketService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.aop.framework.AopContext;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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

    @Resource
    private WebSocketService webSocketService;

    @Resource
    private AsyncGameService asyncGameService;

    @Override
    public String createRoom(UndercoverRoomCreateRequest request) {
        // 验证请求参数
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 如果平民词语或卧底词语为空，从文件中随机读取一对词语
        if (StringUtils.isBlank(request.getCivilianWord()) || StringUtils.isBlank(request.getUndercoverWord())) {
            try {
                // 从文件中读取词语对
                String[] wordPair = getRandomWordPair();
                if (wordPair != null && wordPair.length == 2) {
                    request.setCivilianWord(wordPair[0]);
                    request.setUndercoverWord(wordPair[1]);
                } else {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取词语对失败");
                }
            } catch (IOException e) {
                log.error("读取词语文件失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取词语文件失败");
            }
        }

        // 再次验证词语是否为空
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

        // 使用分布式锁确保同一时间只能有一个房间
        RLock lock = redissonClient.getLock("undercover_room_create_lock");
        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后再试");
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
            room.setGameMode(request.getGameMode() != null ? request.getGameMode() : 1); // 设置游戏模式，默认为常规模式

            // 生成房间ID
            String roomId = UUID.randomUUID().toString().replace("-", "");

            // 存储房间信息到Redis
            try {
                String roomJson = objectMapper.writeValueAsString(room);
                stringRedisTemplate.opsForValue().set(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId),
                        roomJson,
                        60,
                        TimeUnit.MINUTES
                );

                MessageWrapper messageWrapper = getSystemMessageWrapper(loginUser.getUserName() + "创建了一个紧张刺激的谁是卧底房间，大家快来参加吧～");
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.CHAT.getType())
                        .data(messageWrapper).build());

                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.REFRESH_ROOM.getType())
                        .data("").build());

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

    /**
     * 从文件中随机获取一对词语
     * 确保每组词语一天内最多只能使用一次
     *
     * @return 词语对数组，第一个元素为平民词，第二个元素为卧底词
     * @throws IOException 如果读取文件失败
     */
    private String[] getRandomWordPair() throws IOException {
        ClassPathResource resource = new ClassPathResource("undercover-words.txt");
        List<String> wordPairs = new ArrayList<>();
        List<String> availableWordPairs = new ArrayList<>();

        // 获取当天已使用的词语对
        Set<String> usedWordPairs = new HashSet<>();
        String usedWordPairsJson = stringRedisTemplate.opsForValue().get(UndercoverGameRedisKey.USED_WORD_PAIRS);
        if (usedWordPairsJson != null) {
            try {
                usedWordPairs = objectMapper.readValue(usedWordPairsJson, new TypeReference<HashSet<String>>() {
                });
            } catch (JsonProcessingException e) {
                log.error("解析已使用词语对失败", e);
                // 解析失败则创建新的集合
                usedWordPairs = new HashSet<>();
            }
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (StringUtils.isNotBlank(line) && line.contains(",")) {
                    String trimmedLine = line.trim();
                    wordPairs.add(trimmedLine);

                    // 如果该词语对今天未使用过，则添加到可用词语对列表中
                    if (!usedWordPairs.contains(trimmedLine)) {
                        availableWordPairs.add(trimmedLine);
                    }
                }
            }
        }

        // 如果没有可用的词语对（所有词语对都已使用过），则使用所有词语对
        if (availableWordPairs.isEmpty()) {
            if (wordPairs.isEmpty()) {
                return null;
            }
            log.info("所有词语对已在今天使用过，重新使用所有词语对");
            availableWordPairs = new ArrayList<>(wordPairs);

            // 清空已使用的词语对记录
            usedWordPairs.clear();
            try {
                String newUsedWordPairsJson = objectMapper.writeValueAsString(usedWordPairs);
                stringRedisTemplate.opsForValue().set(UndercoverGameRedisKey.USED_WORD_PAIRS, newUsedWordPairsJson, 24, TimeUnit.HOURS);
            } catch (JsonProcessingException e) {
                log.error("序列化已使用词语对失败", e);
            }
        }

        // 随机选择一对可用词语
        String randomPair = availableWordPairs.get(new Random().nextInt(availableWordPairs.size()));

        // 将选择的词语对添加到已使用列表中
        usedWordPairs.add(randomPair);
        try {
            String newUsedWordPairsJson = objectMapper.writeValueAsString(usedWordPairs);
            // 设置24小时过期时间，确保第二天可以重新使用
            stringRedisTemplate.opsForValue().set(UndercoverGameRedisKey.USED_WORD_PAIRS, newUsedWordPairsJson, 24, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.error("序列化已使用词语对失败", e);
        }

        return randomPair.split(",");
    }

    @Override
    public UndercoverRoomVO getActiveRoom() {
        // 获取当前活跃房间ID
        String roomId = stringRedisTemplate.opsForValue().get(UndercoverGameRedisKey.ACTIVE_ROOM);
        if (roomId == null) {
            return null;
        }
        return getRoomById(roomId);
    }

    /**
     * 获取所有房间列表
     *
     * @return 房间列表
     */
    @Override
    public List<UndercoverRoomVO> getAllRooms() {
        List<String> roomIds = new ArrayList<>();
        List<UndercoverRoomVO> roomList = new ArrayList<>();

        // 获取所有以 "fish:undercover:room:" 开头的键
        Set<String> keys = stringRedisTemplate.keys(UndercoverGameRedisKey.BASE_KEY + "roomInfo:*");
        if (keys.isEmpty()) {
            return roomList;
        }

        // 遍历所有房间键，获取房间信息
        for (String key : keys) {
            // 从键中提取房间ID
            // 格式为 "fish:undercover:room:roomId"，需要提取最后一部分作为roomId
            String[] parts = key.split(":");
            if (parts.length < 4) {
                continue;
            }
            String roomId = parts[3];

            // 如果是其他类型的键（如投票记录、结果等），跳过
            if (roomId.contains(":")) {
                continue;
            }

            // 获取房间信息
            UndercoverRoomVO roomVO = getRoomById(roomId);
            if (roomVO != null) {
                roomList.add(roomVO);
            }
        }

        // 按创建时间降序排序，最新创建的房间排在前面
        roomList.sort((r1, r2) -> {
            if (r1.getCreateTime() == null || r2.getCreateTime() == null) {
                return 0;
            }
            return r2.getCreateTime().compareTo(r1.getCreateTime());
        });

        return roomList;
    }

    /**
     * 根据房间ID获取房间信息
     *
     * @param roomId 房间ID
     * @return 房间信息
     */
    @Override
    public UndercoverRoomVO getRoomById(String roomId) {
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
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
            roomVO.setCreatorId(room.getCreatorId());

            // 获取创建者信息（头像和名称）
            if (room.getCreatorId() != null) {
                User creator = userService.getById(room.getCreatorId());
                if (creator != null) {
                    roomVO.setCreatorName(creator.getUserName());
                    roomVO.setCreatorAvatar(creator.getUserAvatar());
                }
            }

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

            // 获取当前用户信息
            if (StpUtil.isLogin()) {
                User currentUser = userService.getLoginUser();
                // 检查当前用户是否在房间中
                if (room.getParticipantIds().contains(currentUser.getId())) {
                    // 获取玩家角色
                    String role = stringRedisTemplate.opsForValue().get(
                            UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_ROLE, currentUser.getId()));

                    // 根据游戏模式设置角色和词语
                    if (room.getGameMode() != null && room.getGameMode() == 2) {
                        // 卧底猜词模式：告知角色，卧底不知道词语
                        roomVO.setRole(role);
                        if ("civilian".equals(role)) {
                            roomVO.setWord(room.getCivilianWord());
                        } else if ("undercover".equals(role)) {
                            roomVO.setWord("无");
                        }
                        // 卧底的词语设为null，前端可以显示为"未知"
                    } else {
                        // 常规模式：不告知角色，所有人都有词语
                        if ("undercover".equals(role)) {
                            roomVO.setWord(room.getUndercoverWord());
                        } else if ("civilian".equals(role)) {
                            roomVO.setWord(room.getCivilianWord());
                        }
                    }
                }
            }

            // 获取游戏结果
            String gameResult = stringRedisTemplate.opsForValue().get(
                    UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_RESULT, roomId));
            if (gameResult != null) {
                roomVO.setGameResult(gameResult);
            }

            // 如果游戏已结束，确保游戏结果不为空
            if (room.getStatus() == RoomStatusEnum.ENDED && StringUtils.isBlank(roomVO.getGameResult())) {
                roomVO.setGameResult("游戏已结束");
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

                // 如果有序列表已存在，也添加到有序列表中
                if (room.getOrderedParticipantIds() != null) {
                    room.getOrderedParticipantIds().add(loginUser.getId());
                } else {
                    // 如果有序列表不存在，创建一个
                    List<Long> orderedIds = new ArrayList<>(room.getParticipantIds());
                    room.setOrderedParticipantIds(orderedIds);
                }

                // 更新房间信息
                String updatedRoomJson = objectMapper.writeValueAsString(room);
                stringRedisTemplate.opsForValue().set(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId),
                        updatedRoomJson,
                        60,
                        TimeUnit.MINUTES
                );

                // 记录用户所在房间
                stringRedisTemplate.opsForValue().set(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_ROOM, loginUser.getId()),
                        roomId,
                        60,
                        TimeUnit.MINUTES
                );
                WSBaseResp<Object> infoResp = WSBaseResp.builder()
                        .type(MessageTypeEnum.INFO.getType())
                        .data("用户" + loginUser.getUserName() + "进入谁是卧底房间中")
                        .build();
                webSocketService.sendToAllOnline(infoResp);

                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.REFRESH_ROOM.getType())
                        .data("").build());

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

        // 验证是否为房间创建者或管理员
        User loginUser = userService.getLoginUser();

        // 获取房间信息
        String roomJson = stringRedisTemplate.opsForValue().get(
                UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId));
        if (roomJson == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "房间不存在");
        }

        UndercoverRoom room;
        try {
            room = objectMapper.readValue(roomJson, UndercoverRoom.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解析房间信息失败");
        }

        // 检查是否为房间创建者或管理员
        boolean isCreator = loginUser.getId().equals(room.getCreatorId());
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());

        if (!isCreator && !isAdmin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有房间创建者或管理员可以开始游戏");
        }

        // 使用分布式锁确保并发安全
        RLock lock = redissonClient.getLock("undercover_room_start_lock:" + roomId);
        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后再试");
            }

            try {

                // 检查房间状态
                if (room.getStatus() != RoomStatusEnum.WAITING) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "房间已开始或已结束");
                }

                // 检查参与者数量
                if (room.getParticipantIds().size() < 3) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "参与者数量不足，至少需要3人");
                }

                // 打乱玩家顺序
                List<Long> shuffledParticipants = new ArrayList<>(room.getParticipantIds());
                Collections.shuffle(shuffledParticipants);
                room.setOrderedParticipantIds(shuffledParticipants);

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
                        60,
                        TimeUnit.MINUTES
                );

                // 根据游戏模式发送不同的提示信息
                String gameStartMessage;
                if (room.getGameMode() != null && room.getGameMode() == 2) {
                    gameStartMessage = "谁是卧底游戏开始啦！这是卧底猜词模式，卧底需要猜出平民的词语！请大家按顺序描述自己的词语";
                } else {
                    gameStartMessage = "谁是卧底游戏开始啦！请大家按顺序描述自己的词语";
                }

                MessageWrapper messageWrapper = getSystemMessageWrapper(gameStartMessage);
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.UNDERCOVER.getType())
                        .data(messageWrapper).build());

                // 直接调用异步方法
                // 委托给异步服务处理
                asyncGameService.startSpeakingAndVoting(roomId);

                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.GAME_STAR.getType())
                        .data("").build());

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

    @NotNull
    private static MessageWrapper getSystemMessageWrapper(String content) {
        Message message = new Message();
        message.setId("-1");
        message.setContent(content);
        Sender sender = new Sender();
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
        message.setTimestamp(Instant.now().toString());

        MessageWrapper messageWrapper = new MessageWrapper();
        messageWrapper.setMessage(message);
        return messageWrapper;
    }

    /**
     * 分配角色
     *
     * @param room 房间信息
     */
    private void assignRoles(UndercoverRoom room) {
        // 使用已经打乱的玩家列表
        List<Long> participants = room.getOrderedParticipantIds();

        // 如果有序列表为空（向后兼容），则使用参与者列表并打乱
        if (participants == null || participants.isEmpty()) {
            participants = new ArrayList<>(room.getParticipantIds());
            Collections.shuffle(participants);
            room.setOrderedParticipantIds(participants);
        }

        // 确定卧底数量（约1/3的玩家，至少1人）
        int undercoverCount = Math.max(1, participants.size() / 3);

        // 清空现有角色分配
        room.getUndercoverIds().clear();
        room.getCivilianIds().clear();

        // 创建一个玩家ID列表的副本，并再次打乱，用于角色分配
        List<Long> shuffledForRoles = new ArrayList<>(participants);
        Collections.shuffle(shuffledForRoles);

        // 分配角色 - 从打乱后的列表中选择卧底
        for (int i = 0; i < shuffledForRoles.size(); i++) {
            Long userId = shuffledForRoles.get(i);
            if (i < undercoverCount) {
                room.getUndercoverIds().add(userId);
                // 存储玩家角色信息
                stringRedisTemplate.opsForValue().set(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_ROLE, userId),
                        "undercover",
                        60,
                        TimeUnit.MINUTES
                );

                // 根据游戏模式发送不同的提示信息

                if (room.getGameMode() != null && room.getGameMode() == 2) {
                    String message = "你是卧底！你需要猜出平民的词语。请仔细观察其他玩家的描述，隐藏好自己的身份。";

                    WSBaseResp<Object> infoResp = WSBaseResp.builder()
                            .type(MessageTypeEnum.INFO.getType())
                            .data(message)
                            .build();
                    webSocketService.sendToUid(infoResp, userId);
                }


            } else {
                room.getCivilianIds().add(userId);
                // 存储玩家角色信息
                stringRedisTemplate.opsForValue().set(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_ROLE, userId),
                        "civilian",
                        60,
                        TimeUnit.MINUTES
                );
                WSBaseResp<Object> infoResp = WSBaseResp.builder()
                        .type(MessageTypeEnum.INFO.getType())
                        .data("你的提示词是：" + room.getCivilianWord())
                        .build();
                webSocketService.sendToUid(infoResp, userId);
            }
        }
    }

    @Override
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
                        asyncGameService.startSpeakingAndVoting(roomId);
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
            if (StringUtils.isBlank(role)) {
                playerVO.setWord("");
            } else if ("undercover".equals(role)) {
                playerVO.setWord(room.getUndercoverWord());
            } else {
                playerVO.setWord(room.getCivilianWord());
            }

            // 设置是否被淘汰
            playerVO.setIsEliminated(room.getEliminatedIds().contains(userId));

            // 如果是卧底猜词模式，且玩家是卧底，设置猜词次数信息
            if (room.getGameMode() != null && room.getGameMode() == 2 && "undercover".equals(role)) {
                // 获取玩家已猜词次数
                String guessCountKey = UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_GUESS_COUNT, roomId, userId);
                String guessCountStr = stringRedisTemplate.opsForValue().get(guessCountKey);
                int guessCount = 0;
                if (guessCountStr != null) {
                    guessCount = Integer.parseInt(guessCountStr);
                }
                playerVO.setGuessCount(guessCount);
                playerVO.setRemainingGuessCount(Math.max(0, UndercoverGameRedisKey.MAX_GUESS_COUNT - guessCount));
            }

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
                        60,
                        TimeUnit.MINUTES
                );

                // 检查游戏是否结束
                boolean isGameOver = checkGameOver(roomId);
                if (isGameOver) {
                    room.setStatus(RoomStatusEnum.ENDED);
                    updatedRoomJson = objectMapper.writeValueAsString(room);
                    stringRedisTemplate.opsForValue().set(
                            UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId),
                            updatedRoomJson,
                            60,
                            TimeUnit.MINUTES
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
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
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
        String roomId = request.getRoomId();
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
                        60,
                        TimeUnit.MINUTES
                );

                // 更新投票计数
                String voteCountKey = UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_VOTE_COUNT, roomId) + ":" + targetId;
                String voteCountStr = stringRedisTemplate.opsForValue().get(voteCountKey);
                int voteCount = 1;
                if (voteCountStr != null) {
                    voteCount = Integer.parseInt(voteCountStr) + 1;
                }
                stringRedisTemplate.opsForValue().set(voteCountKey, String.valueOf(voteCount), 60, TimeUnit.MINUTES);

                // 标记用户已投票
                stringRedisTemplate.opsForValue().set(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_VOTED, roomId, loginUser.getId()),
                        "1",
                        60,
                        TimeUnit.MINUTES
                );
                MessageWrapper messageWrapper = getSystemMessageWrapper(loginUser.getUserName() + "用户已完成投票");

                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.UNDERCOVER.getType())
                        .data(messageWrapper).build());

                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.REFRESH_ROOM.getType())
                        .data("").build());
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

            // 如果是卧底猜词模式，获取玩家角色和猜词次数
            if (room.getGameMode() != null && room.getGameMode() == 2) {
                // 获取玩家角色
                String role = stringRedisTemplate.opsForValue().get(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_ROLE, userId));

                // 如果是卧底，设置猜词次数信息
                if ("undercover".equals(role)) {
                    // 获取玩家已猜词次数
                    String guessCountKey = UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_GUESS_COUNT, roomId, userId);
                    String guessCountStr = stringRedisTemplate.opsForValue().get(guessCountKey);
                    int guessCount = 0;
                    if (guessCountStr != null) {
                        guessCount = Integer.parseInt(guessCountStr);
                    }
                    playerDetailVO.setGuessCount(guessCount);
                    playerDetailVO.setRemainingGuessCount(Math.max(0, UndercoverGameRedisKey.MAX_GUESS_COUNT - guessCount));
                }
            }

            return playerDetailVO;
        } catch (JsonProcessingException e) {
            log.error("解析房间信息失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取玩家信息失败");
        }
    }

    @Override
    public List<UndercoverPlayerDetailVO> getRoomPlayersDetail(String roomId) {
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

            // 使用有序的参与者ID列表
            List<Long> participantIds;
            if (room.getOrderedParticipantIds() != null && !room.getOrderedParticipantIds().isEmpty()) {
                participantIds = room.getOrderedParticipantIds();
            } else {
                // 向后兼容，如果没有有序列表，则使用无序集合
                participantIds = new ArrayList<>(room.getParticipantIds());
            }

            // 获取所有参与者的详细信息
            for (Long userId : participantIds) {
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

                // 如果是卧底猜词模式，获取玩家角色和猜词次数
                if (room.getGameMode() != null && room.getGameMode() == 2) {
                    // 获取玩家角色
                    String role = stringRedisTemplate.opsForValue().get(
                            UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_ROLE, userId));

                    // 如果是卧底，设置猜词次数信息
                    if ("undercover".equals(role)) {
                        // 获取玩家已猜词次数
                        String guessCountKey = UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_GUESS_COUNT, roomId, userId);
                        String guessCountStr = stringRedisTemplate.opsForValue().get(guessCountKey);
                        int guessCount = 0;
                        if (guessCountStr != null) {
                            guessCount = Integer.parseInt(guessCountStr);
                        }
                        playerDetailVO.setGuessCount(guessCount);
                        playerDetailVO.setRemainingGuessCount(Math.max(0, UndercoverGameRedisKey.MAX_GUESS_COUNT - guessCount));
                    }
                }

                playerDetails.add(playerDetailVO);
            }

            return playerDetails;
        } catch (JsonProcessingException e) {
            log.error("解析房间信息失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取房间玩家信息失败");
        }
    }

    @Override
    public boolean removeActiveRoom(String roomId) {
        // 验证是否为管理员
        User loginUser = userService.getLoginUser();
        if (!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有管理员可以移除房间");
        }

        // 使用分布式锁确保操作的原子性
        RLock lock = redissonClient.getLock("undercover_room_remove_lock");
        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后再试");
            }

            // 获取房间信息
            String roomJson = stringRedisTemplate.opsForValue().get(
                    UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId));
            if (roomJson == null) {
                // 房间不存在，清除活跃房间记录
                stringRedisTemplate.delete(UndercoverGameRedisKey.ACTIVE_ROOM);
                return true;
            }

            try {
                UndercoverRoom room = objectMapper.readValue(roomJson, UndercoverRoom.class);

                // 如果房间还在游戏中，先通知玩家游戏被管理员强制结束
                if (room.getStatus() == RoomStatusEnum.PLAYING) {
                    // 创建系统消息，通知所有玩家房间被移除
                    WSBaseResp<Object> infoResp = WSBaseResp.builder()
                            .type(MessageTypeEnum.INFO.getType())
                            .data("游戏房间已被管理员移除")
                            .build();
                    webSocketService.sendToAllOnline(infoResp);
                }

                // 删除房间相关的所有信息
                // 1. 删除房间信息
                stringRedisTemplate.delete(UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId));

                // 2. 删除房间投票记录
                stringRedisTemplate.delete(UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_VOTES, roomId));

                // 3. 删除房间结果
                stringRedisTemplate.delete(UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_RESULT, roomId));

                // 4. 清除所有玩家在该房间中的信息
                for (Long playerId : room.getParticipantIds()) {
                    // 删除玩家角色信息
                    stringRedisTemplate.delete(UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_ROLE, playerId));

                    // 删除玩家所在房间信息
                    stringRedisTemplate.delete(UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_ROOM, playerId));

                    // 删除玩家的投票状态
                    stringRedisTemplate.delete(UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_VOTED, roomId, playerId));

                    // 删除玩家收到的投票计数
                    String voteCountKey = UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_VOTE_COUNT, roomId) + ":" + playerId;
                    stringRedisTemplate.delete(voteCountKey);

                    // 删除玩家猜词次数记录
                    stringRedisTemplate.delete(
                            UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_GUESS_COUNT, roomId, playerId)
                    );
                }

                // 5. 删除活跃房间记录
                stringRedisTemplate.delete(UndercoverGameRedisKey.ACTIVE_ROOM);

                // 6. 通知客户端刷新房间状态
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.REFRESH_ROOM.getType())
                        .data("").build());

                return true;
            } catch (JsonProcessingException e) {
                log.error("解析房间信息失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "移除房间失败");
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
    public boolean guessWord(UndercoverGuessRequest request) {
        // 验证参数
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "猜词请求不能为空");
        }
        String roomId = request.getRoomId();
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        String guessWord = request.getGuessWord();
        if (StringUtils.isBlank(guessWord)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "猜测词语不能为空");
        }

        // 验证用户登录状态
        User loginUser = userService.getLoginUser();

        // 使用分布式锁确保并发安全
        RLock lock = redissonClient.getLock("undercover_room_guess_lock:" + roomId);
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

                // 检查是否为卧底猜词模式
                if (room.getGameMode() == null || room.getGameMode() != 2) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前游戏模式不支持猜词功能");
                }

                // 检查用户是否在房间中
                if (!room.getParticipantIds().contains(loginUser.getId())) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户不在房间中");
                }

                // 检查用户是否已被淘汰
                if (room.getEliminatedIds().contains(loginUser.getId())) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户已被淘汰，无法猜词");
                }

                // 检查用户是否是卧底
                if (!room.getUndercoverIds().contains(loginUser.getId())) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有卧底才能猜词");
                }

                // 获取用户已猜词次数
                String guessCountKey = UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_GUESS_COUNT, roomId, loginUser.getId());
                String guessCountStr = stringRedisTemplate.opsForValue().get(guessCountKey);
                int guessCount = 0;
                if (guessCountStr != null) {
                    guessCount = Integer.parseInt(guessCountStr);
                }

                // 检查是否已达到猜词上限
                if (guessCount >= UndercoverGameRedisKey.MAX_GUESS_COUNT) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已达到猜词上限（" + UndercoverGameRedisKey.MAX_GUESS_COUNT + "次），无法继续猜词");
                }

                // 增加猜词次数
                guessCount++;
                stringRedisTemplate.opsForValue().set(guessCountKey, String.valueOf(guessCount), 60, TimeUnit.MINUTES);

                // 检查猜测是否正确
                boolean isCorrect = guessWord.trim().equals(room.getCivilianWord().trim());

                // 如果猜对了，结束游戏并宣布卧底胜利
                if (isCorrect) {
                    // 更新房间状态
                    room.setStatus(RoomStatusEnum.ENDED);

                    // 保存更新后的房间信息
                    String updatedRoomJson = objectMapper.writeValueAsString(room);
                    stringRedisTemplate.opsForValue().set(
                            UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId),
                            updatedRoomJson,
                            60,
                            TimeUnit.MINUTES
                    );
                    // 删除活跃房间记录
                    stringRedisTemplate.delete(UndercoverGameRedisKey.ACTIVE_ROOM);

                    // 发送游戏结束消息
                    String userName = loginUser.getUserName();
                    MessageWrapper messageWrapper = getSystemMessageWrapper(
                            "卧底" + userName + "成功猜出平民词「" + room.getCivilianWord() + "」！卧底获胜！"
                    );

                    webSocketService.sendToAllOnline(WSBaseResp.builder()
                            .type(MessageTypeEnum.UNDERCOVER.getType())
                            .data(messageWrapper).build());

                    webSocketService.sendToAllOnline(WSBaseResp.builder()
                            .type(MessageTypeEnum.REFRESH_ROOM.getType())
                            .data("").build());

                    return true;
                } else {
                    // 猜错了，发送提示消息
                    String message;
                    if (guessCount >= UndercoverGameRedisKey.MAX_GUESS_COUNT) {
                        // 已达到最大猜词次数，淘汰该卧底
                        room.getEliminatedIds().add(loginUser.getId());

                        // 更新房间信息
                        String updatedRoomJson = objectMapper.writeValueAsString(room);
                        stringRedisTemplate.opsForValue().set(
                                UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId),
                                updatedRoomJson,
                                60,
                                TimeUnit.MINUTES
                        );

                        message = "卧底" + loginUser.getUserName() + "猜词「" + guessWord + "」错误，已达到" + UndercoverGameRedisKey.MAX_GUESS_COUNT + "次猜词上限，被淘汰出局！";

                        // 检查游戏是否结束
                        boolean isGameOver = checkGameOver(roomId);
                        if (isGameOver) {
                            endGame(roomId);
                        }
                    } else {
                        message = "卧底猜词「" + guessWord + "」错误，还有" + (UndercoverGameRedisKey.MAX_GUESS_COUNT - guessCount) + "次猜词机会！";
                    }

                    MessageWrapper messageWrapper = getSystemMessageWrapper(message);
                    webSocketService.sendToAllOnline(WSBaseResp.builder()
                            .type(MessageTypeEnum.UNDERCOVER.getType())
                            .data(messageWrapper).build());

                    webSocketService.sendToAllOnline(WSBaseResp.builder()
                            .type(MessageTypeEnum.REFRESH_ROOM.getType())
                            .data("").build());

                    return false;
                }

            } catch (JsonProcessingException e) {
                log.error("解析房间信息失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "处理猜词请求失败");
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
     * 将词语对添加到已使用列表中
     *
     * @param wordPair 词语对，格式为"平民词,卧底词"
     */
    private void addWordPairToUsedList(String wordPair) {
        if (StringUtils.isBlank(wordPair) || !wordPair.contains(",")) {
            return;
        }

        // 获取当天已使用的词语对
        Set<String> usedWordPairs = new HashSet<>();
        String usedWordPairsJson = stringRedisTemplate.opsForValue().get(UndercoverGameRedisKey.USED_WORD_PAIRS);
        if (usedWordPairsJson != null) {
            try {
                usedWordPairs = objectMapper.readValue(usedWordPairsJson, new TypeReference<HashSet<String>>() {
                });
            } catch (JsonProcessingException e) {
                log.error("解析已使用词语对失败", e);
                // 解析失败则创建新的集合
                usedWordPairs = new HashSet<>();
            }
        }

        // 将词语对添加到已使用列表中
        usedWordPairs.add(wordPair);
        try {
            String newUsedWordPairsJson = objectMapper.writeValueAsString(usedWordPairs);
            // 设置24小时过期时间，确保第二天可以重新使用
            stringRedisTemplate.opsForValue().set(UndercoverGameRedisKey.USED_WORD_PAIRS, newUsedWordPairsJson, 24, TimeUnit.HOURS);
            log.info("词语对[{}]已添加到已使用列表中", wordPair);
        } catch (JsonProcessingException e) {
            log.error("序列化已使用词语对失败", e);
        }
    }

    @Override
    public boolean quitRoom(String roomId) {
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }

        // 验证用户登录状态
        User loginUser = userService.getLoginUser();

        // 使用分布式锁确保并发安全
        RLock lock = redissonClient.getLock("undercover_room_quit_lock:" + roomId);
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

                // 检查用户是否在房间中
                if (!room.getParticipantIds().contains(loginUser.getId())) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户不在房间中");
                }

                // 根据房间状态处理不同情况
                if (room.getStatus() == RoomStatusEnum.WAITING) {
                    // 如果房间处于等待状态，直接退出
                    room.getParticipantIds().remove(loginUser.getId());

                    // 从有序列表中移除
                    if (room.getOrderedParticipantIds() != null) {
                        room.getOrderedParticipantIds().remove(loginUser.getId());
                    }

                    // 如果是创建者退出，且还有其他人在房间中，则随机选择一个人成为新的创建者
                    if (loginUser.getId().equals(room.getCreatorId()) && !room.getParticipantIds().isEmpty()) {
                        Long newCreatorId = room.getParticipantIds().iterator().next();
                        room.setCreatorId(newCreatorId);

                        // 通知新的创建者
                        WSBaseResp<Object> infoResp = WSBaseResp.builder()
                                .type(MessageTypeEnum.INFO.getType())
                                .data("房主已退出，你成为了新的房主！")
                                .build();
                        webSocketService.sendToUid(infoResp, newCreatorId);
                    }
                    // 如果创建者退出且没有其他人，则房间结束
                    else if (loginUser.getId().equals(room.getCreatorId()) && room.getParticipantIds().isEmpty()) {
                        // 清除活跃房间
                        stringRedisTemplate.delete(UndercoverGameRedisKey.ACTIVE_ROOM);
                        // 删除房间信息
                        stringRedisTemplate.delete(UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId));

                        // 删除玩家所在房间信息
                        stringRedisTemplate.delete(UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_ROOM, loginUser.getId()));

                        MessageWrapper messageWrapper = getSystemMessageWrapper("谁是卧底游戏房间已关闭");
                        webSocketService.sendToAllOnline(WSBaseResp.builder()
                                .type(MessageTypeEnum.UNDERCOVER.getType())
                                .data(messageWrapper).build());

                        webSocketService.sendToAllOnline(WSBaseResp.builder()
                                .type(MessageTypeEnum.REFRESH_ROOM.getType())
                                .data("").build());

                        return true;
                    }
                } else if (room.getStatus() == RoomStatusEnum.PLAYING) {
                    // 如果游戏已经开始
                    // 1. 从参与者列表中移除
                    room.getParticipantIds().remove(loginUser.getId());

                    // 从有序列表中移除
                    if (room.getOrderedParticipantIds() != null) {
                        room.getOrderedParticipantIds().remove(loginUser.getId());
                    }

                    // 2. 根据用户角色从对应列表中移除
                    boolean isUndercover = room.getUndercoverIds().contains(loginUser.getId());
                    boolean isCivilian = room.getCivilianIds().contains(loginUser.getId());

                    if (isUndercover) {
                        room.getUndercoverIds().remove(loginUser.getId());
                        room.getEliminatedIds().add(loginUser.getId());
                    } else if (isCivilian) {
                        room.getCivilianIds().remove(loginUser.getId());
                        room.getEliminatedIds().add(loginUser.getId());
                    }

                    // 3. 计算剩余卧底和平民数量
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

                    // 4. 判断游戏是否结束
                    boolean shouldEndGame = false;
                    String gameResult = "";

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

                        gameResult = "平民获胜！所有卧底已退出游戏！卧底是：" + undercoverNames
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
                        // 游戏继续，显示谁退出了
                        String userRole = isUndercover ? "卧底" : "平民";
                        gameResult = "玩家【" + loginUser.getUserName() + "】退出了游戏，他是" + userRole
                                + "！剩余平民" + remainingCivilians + "人，卧底" + remainingUndercovers + "人，游戏继续...";

                        // 保存退出信息但不结束游戏
                        stringRedisTemplate.opsForValue().set(
                                UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_RESULT, roomId),
                                gameResult,
                                60,
                                TimeUnit.MINUTES
                        );
                    }

                    if (shouldEndGame) {
                        stringRedisTemplate.delete(UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId));

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
                            String voteCountKey = UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_VOTE_COUNT, roomId) + ":" + playerId;
                            stringRedisTemplate.delete(voteCountKey);

                            // 删除玩家猜词次数记录
                            stringRedisTemplate.delete(
                                    UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_GUESS_COUNT, roomId, playerId)
                            );
                        }

                        // 删除房间的投票记录
                        stringRedisTemplate.delete(
                                UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_VOTES, roomId)
                        );
                    }
                } else {
                    // 游戏已结束，仅删除玩家所在房间信息
                    stringRedisTemplate.delete(
                            UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_ROOM, loginUser.getId())
                    );
                    return true;
                }

                // 删除玩家角色信息
                stringRedisTemplate.delete(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_ROLE, loginUser.getId())
                );

                // 删除玩家所在房间信息
                stringRedisTemplate.delete(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_ROOM, loginUser.getId())
                );

                // 删除玩家的投票状态
                stringRedisTemplate.delete(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.PLAYER_VOTED, roomId, loginUser.getId())
                );

                // 更新房间信息
                String updatedRoomJson = objectMapper.writeValueAsString(room);
                stringRedisTemplate.opsForValue().set(
                        UndercoverGameRedisKey.getKey(UndercoverGameRedisKey.ROOM_INFO, roomId),
                        updatedRoomJson,
                        60,
                        TimeUnit.MINUTES
                );

                // 发送退出消息
                MessageWrapper messageWrapper = getSystemMessageWrapper(loginUser.getUserName() + "退出了谁是卧底游戏房间");
                messageWrapper.getMessage().setRoomId(roomId);

                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.UNDERCOVER.getType())
                        .data(messageWrapper).build());

                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.REFRESH_ROOM.getType())
                        .data("").build());

                return true;
            } catch (JsonProcessingException e) {
                log.error("解析房间信息失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出房间失败");
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
     * 按房间存活玩家顺序依次发送发言提醒，发送间隔20秒，全部玩家发送完毕后提醒投票，投票时间30秒后自动结算
     *
     * @param roomId 房间ID
     */
    @Override
    public void startSpeakingAndVoting(String roomId) {
        // 委托给异步服务处理
        asyncGameService.startSpeakingAndVoting(roomId);
    }
} 