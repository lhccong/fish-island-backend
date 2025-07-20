package com.cong.fishisland.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.DrawGameRedisKey;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.game.DrawDataSaveRequest;
import com.cong.fishisland.model.dto.game.DrawGuessRequest;
import com.cong.fishisland.model.dto.game.DrawRoomCreateRequest;
import com.cong.fishisland.model.entity.game.DrawRoom;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.enums.MessageTypeEnum;
import com.cong.fishisland.model.enums.RoomStatusEnum;
import com.cong.fishisland.model.vo.game.DrawGuessVO;
import com.cong.fishisland.model.vo.game.DrawPlayerVO;
import com.cong.fishisland.model.vo.game.DrawRoomVO;
import com.cong.fishisland.model.ws.request.Message;
import com.cong.fishisland.model.ws.request.MessageWrapper;
import com.cong.fishisland.model.ws.request.Sender;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import com.cong.fishisland.service.DrawGameService;
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

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 你画我猜游戏服务实现
 *
 * @author cong
 */
@Service
@Slf4j
public class DrawGameServiceImpl implements DrawGameService {

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

    @Override
    public String createRoom(DrawRoomCreateRequest request) {
        // 验证请求参数
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 验证最大人数
        if (request.getMaxPlayers() == null || request.getMaxPlayers() < 2) {
            request.setMaxPlayers(8);
        }
        if (request.getMaxPlayers() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间最大人数不能超过20人");
        }

        // 验证总轮数
        if (request.getTotalRounds() == null || request.getTotalRounds() < 1) {
            request.setTotalRounds(10);
        }
        if (request.getTotalRounds() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "总轮数不能超过20轮");
        }

        // 设置房间模式，默认为轮换模式
        if (request.getCreatorOnlyMode() == null) {
            request.setCreatorOnlyMode(false);
        }

        // 验证是否登录
        User loginUser = userService.getLoginUser();

        // 使用分布式锁确保同一时间只能有一个房间被创建
        RLock lock = redissonClient.getLock("draw_room_create_lock");
        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后再试");
            }

            // 创建新房间
            DrawRoom room = new DrawRoom();
            room.setStatus(RoomStatusEnum.WAITING);
            room.setParticipantIds(new HashSet<>());
            room.setCreateTime(new Date());
            room.setCreatorId(loginUser.getId());
            room.setMaxPlayers(request.getMaxPlayers());
            room.setTotalRounds(request.getTotalRounds());
            room.setRoundDuration(600);
            room.setCorrectGuessIds(new HashSet<>());
            room.setCurrentDrawerId(loginUser.getId());
            room.setCreatorOnlyMode(request.getCreatorOnlyMode());
            room.setWordType(request.getWordType());
            // 如果自定义词语为空，从词库中随机选择一个
            try {
                Map<String, String> wordData = getRandomWordWithHint(request.getWordType());
                room.setCurrentWord(wordData.get("word"));
                room.setWordHint(wordData.get("hint"));
            } catch (IOException e) {
                log.error("读取词语文件失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取词语文件失败");
            }


            // 添加创建者到参与者列表
            room.getParticipantIds().add(loginUser.getId());

            // 生成房间ID
            String roomId = UUID.randomUUID().toString().replace("-", "");

            // 存储房间信息到Redis
            try {
                String roomJson = objectMapper.writeValueAsString(room);
                stringRedisTemplate.opsForValue().set(
                        DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_INFO, roomId),
                        roomJson,
                        60,
                        TimeUnit.MINUTES
                );

                // 记录用户所在房间
                stringRedisTemplate.opsForValue().set(
                        DrawGameRedisKey.getKey(DrawGameRedisKey.PLAYER_ROOM, String.valueOf(loginUser.getId())),
                        roomId,
                        60,
                        TimeUnit.MINUTES
                );

                // 发送系统消息
                MessageWrapper messageWrapper = getSystemMessageWrapper(loginUser.getUserName() + "创建了一个你画我猜房间，大家快来参加吧～");
                messageWrapper.getMessage().setRoomId(roomId);
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.CHAT.getType())
                        .data(messageWrapper).build());

                // 更新轻量级房间列表缓存
                updateRoomListCache();

                // 通知前端刷新房间列表
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.REFRESH_DRAW.getType())
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
     * 从文件中随机获取一个词语及其提示词
     * 确保当天已使用过的词语不会再次出现
     *
     * @param wordType 词库类型，如果为空则使用默认词库
     * @return 包含词语和提示词的Map，key为"word"和"hint"
     * @throws IOException 如果读取文件失败
     */
    private Map<String, String> getRandomWordWithHint(String wordType) throws IOException {
        String fileName = "draw-words.txt";
        if (StringUtils.isNotBlank(wordType)) {
            fileName = "draw-words-" + wordType + ".txt";
        }
        ClassPathResource resource = new ClassPathResource(fileName);
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (StringUtils.isNotBlank(line)) {
                    String trimmedLine = line.trim();
                    lines.add(trimmedLine);
                }
            }
        } catch (IOException e) {
            log.error("读取词库文件 {} 失败，将使用默认词库", fileName, e);
            // 如果指定的词库文件不存在，尝试使用默认词库
            if (!fileName.equals("draw-words.txt")) {
                return getRandomWordWithHint(null);
            }
            throw e;
        }

        if (lines.isEmpty()) {
            // 如果词库为空，返回默认词语和提示词
            Map<String, String> defaultResult = new HashMap<>();
            defaultResult.put("word", "苹果");
            defaultResult.put("hint", "水果");
            return defaultResult;
        }

        // 获取当天已使用的词语列表
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String usedWordsKey = DrawGameRedisKey.getKey(DrawGameRedisKey.USED_WORDS, today);
        Set<String> usedWords = stringRedisTemplate.opsForSet().members(usedWordsKey);

        // 如果所有词语都已使用过，则清空已使用列表
        if (usedWords != null && usedWords.size() >= lines.size()) {
            stringRedisTemplate.delete(usedWordsKey);
            usedWords = new HashSet<>();
        }

        // 筛选未使用的词语
        List<String> availableLines = new ArrayList<>();
        for (String line : lines) {
            String word = line.split(",")[0];
            if (usedWords == null || !usedWords.contains(word)) {
                availableLines.add(line);
            }
        }

        // 如果没有可用词语，使用所有词语
        if (availableLines.isEmpty()) {
            availableLines = lines;
        }

        // 随机选择一行
        String selectedLine = availableLines.get(new Random().nextInt(availableLines.size()));
        String[] parts = selectedLine.split(",");

        // 将选中的词语添加到已使用列表
        stringRedisTemplate.opsForSet().add(usedWordsKey, parts[0]);
        // 设置过期时间为明天凌晨00:00:00
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        tomorrow.set(Calendar.HOUR_OF_DAY, 0);
        tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.SECOND, 0);
        tomorrow.set(Calendar.MILLISECOND, 0);
        long expireSeconds = (tomorrow.getTimeInMillis() - System.currentTimeMillis()) / 1000;
        stringRedisTemplate.expire(usedWordsKey, expireSeconds, TimeUnit.SECONDS);

        Map<String, String> result = new HashMap<>();
        result.put("word", parts[0]);
        result.put("hint", parts.length > 1 ? parts[1] : "未知类别");

        return result;
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

    @Override
    public boolean joinRoom(String roomId) {
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }

        // 验证用户登录状态
        User loginUser = userService.getLoginUser();

        // 使用分布式锁确保并发安全
        RLock lock = redissonClient.getLock("draw_room_join_lock:" + roomId);
        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后再试");
            }

            // 获取房间信息
            String roomJson = stringRedisTemplate.opsForValue().get(
                    DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_INFO, roomId));
            if (roomJson == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "房间不存在");
            }

            try {
                DrawRoom room = objectMapper.readValue(roomJson, DrawRoom.class);

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
                        DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_INFO, roomId),
                        updatedRoomJson,
                        60,
                        TimeUnit.MINUTES
                );

                // 记录用户所在房间
                stringRedisTemplate.opsForValue().set(
                        DrawGameRedisKey.getKey(DrawGameRedisKey.PLAYER_ROOM, String.valueOf(loginUser.getId())),
                        roomId,
                        60,
                        TimeUnit.MINUTES
                );

                // 更新轻量级房间列表缓存
                updateRoomListCache();

                // 通知前端刷新房间列表
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.REFRESH_DRAW.getType())
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

    /**
     * 更新轻量级房间列表缓存
     * 不包含绘画数据，提高查询效率
     */
    private void updateRoomListCache() {
        List<DrawRoomVO> roomList = new ArrayList<>();

        // 获取所有房间信息
        Set<String> keys = stringRedisTemplate.keys(DrawGameRedisKey.BASE_KEY + "roomInfo:*");
        if (keys.isEmpty()) {
            // 如果没有房间，清空缓存
            stringRedisTemplate.delete(DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_LIST));
            return;
        }

        // 遍历所有房间键，获取轻量级房间信息
        for (String key : keys) {
            String[] parts = key.split(":");
            if (parts.length < 4) {
                continue;
            }
            String roomId = parts[3];

            // 获取房间信息，但不包含绘画数据
            String roomJson = stringRedisTemplate.opsForValue().get(key);
            if (roomJson != null) {
                try {
                    DrawRoom room = objectMapper.readValue(roomJson, DrawRoom.class);
                    DrawRoomVO roomVO = convertRoomToVO(room, roomId);
                    // 不设置绘画数据
                    roomVO.setDrawData(null);
                    roomList.add(roomVO);
                } catch (JsonProcessingException e) {
                    log.error("解析房间信息失败", e);
                }
            }
        }

        // 按创建时间降序排序
        roomList.sort((r1, r2) -> {
            if (r1.getCreateTime() == null || r2.getCreateTime() == null) {
                return 0;
            }
            return r2.getCreateTime().compareTo(r1.getCreateTime());
        });

        // 缓存轻量级房间列表
        try {
            String roomListJson = objectMapper.writeValueAsString(roomList);
            stringRedisTemplate.opsForValue().set(
                    DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_LIST),
                    roomListJson,
                    30, // 缓存30秒
                    TimeUnit.SECONDS
            );
        } catch (JsonProcessingException e) {
            log.error("序列化房间列表失败", e);
        }
    }

    @Override
    public DrawRoomVO getRoomById(String roomId) {
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }

        // 获取房间信息
        String roomJson = stringRedisTemplate.opsForValue().get(
                DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_INFO, roomId));
        if (roomJson == null) {
            return null;
        }

        try {
            DrawRoom room = objectMapper.readValue(roomJson, DrawRoom.class);
            DrawRoomVO roomVO = convertRoomToVO(room, roomId);

            // 获取绘画数据
            String drawData = stringRedisTemplate.opsForValue().get(
                    DrawGameRedisKey.getKey(DrawGameRedisKey.DRAW_DATA, roomId));
            roomVO.setDrawData(drawData);

            // 获取当前用户信息
            if (StpUtil.isLogin()) {
                User currentUser = userService.getLoginUser();
                // 检查当前用户是否有权限看到词语
                if (currentUser.getId().equals(room.getCurrentDrawerId())) {
                    roomVO.setCurrentWord(room.getCurrentWord());
                } else {
                    // 对非绘画者隐藏词语
                    roomVO.setCurrentWord(null);
                }

                // 提示词对所有人可见
                roomVO.setWordHint(room.getWordHint());
            }

            return roomVO;
        } catch (JsonProcessingException e) {
            log.error("解析房间信息失败", e);
            return null;
        }
    }

    @Override
    public List<DrawRoomVO> getAllRooms() {
        // 直接从轻量级房间列表缓存中获取数据
        String roomListJson = stringRedisTemplate.opsForValue().get(DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_LIST));
        if (roomListJson != null) {
            try {
                return objectMapper.readValue(roomListJson, new TypeReference<List<DrawRoomVO>>() {
                });
            } catch (JsonProcessingException e) {
                log.error("解析房间列表失败", e);
                // 解析失败，继续使用原有方式获取
            }
        }

        // 如果缓存不存在，则更新并获取缓存
        updateRoomListCache();

        // 再次尝试从缓存获取
        roomListJson = stringRedisTemplate.opsForValue().get(DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_LIST));
        if (roomListJson != null) {
            try {
                return objectMapper.readValue(roomListJson, new TypeReference<List<DrawRoomVO>>() {
                });
            } catch (JsonProcessingException e) {
                log.error("解析房间列表失败", e);
            }
        }

        // 如果仍然失败，返回空列表
        return new ArrayList<>();
    }

    /**
     * 将DrawRoom实体转换为DrawRoomVO
     *
     * @param room   房间实体
     * @param roomId 房间ID
     * @return 房间VO
     */
    private DrawRoomVO convertRoomToVO(DrawRoom room, String roomId) {
        DrawRoomVO roomVO = new DrawRoomVO();
        BeanUtils.copyProperties(room, roomVO);
        roomVO.setRoomId(roomId);
        roomVO.setCurrentPlayers(room.getParticipantIds().size());

        // 获取创建者信息
        if (room.getCreatorId() != null) {
            User creator = userService.getById(room.getCreatorId());
            if (creator != null) {
                roomVO.setCreatorName(creator.getUserName());
                roomVO.setCreatorAvatar(creator.getUserAvatar());
            }
        }

        // 获取当前绘画者信息
        if (room.getCurrentDrawerId() != null) {
            User drawer = userService.getById(room.getCurrentDrawerId());
            if (drawer != null) {
                roomVO.setCurrentDrawerName(drawer.getUserName());
            }
        }

        // 获取房间内所有玩家详细信息
        List<DrawPlayerVO> participants = new ArrayList<>();
        for (Long userId : room.getParticipantIds()) {
            User user = userService.getById(userId);
            if (user != null) {
                DrawPlayerVO playerVO = new DrawPlayerVO();
                playerVO.setUserId(userId);
                playerVO.setUserName(user.getUserName());
                playerVO.setUserAvatar(user.getUserAvatar());
                playerVO.setIsCreator(userId.equals(room.getCreatorId()));
                playerVO.setIsCurrentDrawer(userId.equals(room.getCurrentDrawerId()));
                playerVO.setHasGuessedCorrectly(room.getCorrectGuessIds().contains(userId));

                // 获取玩家积分
                String playerScoreKey = DrawGameRedisKey.getKey(DrawGameRedisKey.PLAYER_SCORE, roomId, userId.toString());
                String scoreStr = stringRedisTemplate.opsForValue().get(playerScoreKey);
                int score = 0;
                if (scoreStr != null) {
                    score = Integer.parseInt(scoreStr);
                }
                playerVO.setScore(score);

                participants.add(playerVO);
            }
        }
        roomVO.setParticipants(participants);

        // 获取已猜中的玩家信息
        List<DrawPlayerVO> correctGuessPlayers = participants.stream()
                .filter(DrawPlayerVO::getHasGuessedCorrectly)
                .collect(Collectors.toList());
        roomVO.setCorrectGuessPlayers(correctGuessPlayers);

        // 设置房间模式
        roomVO.setCreatorOnlyMode(room.getCreatorOnlyMode());

        return roomVO;
    }

    @Override
    public boolean saveDrawData(DrawDataSaveRequest request) {
        // 验证参数
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        String roomId = request.getRoomId();
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }
        String drawData = request.getDrawData();
        if (StringUtils.isBlank(drawData)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "绘画数据不能为空");
        }

        // 验证用户登录状态
        User loginUser = userService.getLoginUser();

        // 使用分布式锁确保并发安全
        RLock lock = redissonClient.getLock("draw_room_save_data_lock:" + roomId);
        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后再试");
            }

            // 获取房间信息
            String roomJson = stringRedisTemplate.opsForValue().get(
                    DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_INFO, roomId));
            if (roomJson == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "房间不存在");
            }

            try {
                DrawRoom room = objectMapper.readValue(roomJson, DrawRoom.class);

                // 检查房间状态
                if (room.getStatus() != RoomStatusEnum.PLAYING && room.getStatus() != RoomStatusEnum.WAITING) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "房间已结束，无法绘画");
                }

                // 检查用户是否在房间中
                if (!room.getParticipantIds().contains(loginUser.getId())) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户不在房间中");
                }

                // 验证权限：只有房主或当前绘画者可以绘画
                if (!loginUser.getId().equals(room.getCreatorId()) && !loginUser.getId().equals(room.getCurrentDrawerId())) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是房主或当前绘画者，无权绘画");
                }

                // 保存绘画数据
                stringRedisTemplate.opsForValue().set(
                        DrawGameRedisKey.getKey(DrawGameRedisKey.DRAW_DATA, roomId),
                        drawData,
                        60,
                        TimeUnit.MINUTES
                );


                // 通知前端刷新绘画数据
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.REFRESH_DRAW.getType())
                        .data(roomId).build());

                //发送提示
                String message = "绘画者【"+loginUser.getUserName() + "】绘画完成大家快来猜猜是什么";
                MessageWrapper messageWrapper = getSystemMessageWrapper(message);
                messageWrapper.getMessage().setRoomId(roomId);

                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.DRAW.getType())
                        .data(messageWrapper).build());

                return true;
            } catch (JsonProcessingException e) {
                log.error("解析房间信息失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存绘画数据失败");
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
    public DrawGuessVO guessWord(DrawGuessRequest request) {
        // 验证参数
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
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
        RLock lock = redissonClient.getLock("draw_room_guess_lock:" + roomId);
        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后再试");
            }

            // 获取房间信息
            String roomJson = stringRedisTemplate.opsForValue().get(
                    DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_INFO, roomId));
            if (roomJson == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "房间不存在");
            }

            try {
                DrawRoom room = objectMapper.readValue(roomJson, DrawRoom.class);

                // 检查房间状态
                if (room.getStatus() != RoomStatusEnum.PLAYING) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "游戏未开始或已结束，无法猜词");
                }

                // 检查用户是否在房间中
                if (!room.getParticipantIds().contains(loginUser.getId())) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户不在房间中");
                }

                // 检查绘画数据是否为空
                String drawData = stringRedisTemplate.opsForValue().get(
                        DrawGameRedisKey.getKey(DrawGameRedisKey.DRAW_DATA, roomId));
                if (StringUtils.isBlank(drawData) && !loginUser.getId().equals(room.getCreatorId())) {
                    MessageWrapper userMessage = request.getMessageWrapper();

                    String contentWord = userMessage.getMessage().getContent();

                    userMessage.getMessage().setContent(contentWord);

                    userMessage.getMessage().setRoomId(roomId);
                    webSocketService.sendToAllOnline(WSBaseResp.builder()
                            .type(MessageTypeEnum.DRAW.getType())
                            .data(userMessage).build(), loginUser.getId());

                    //发送提示
                    String message = loginUser.getUserName() + "【绘画用户绘画中，请等下再猜喔】";
                    MessageWrapper messageWrapper = getSystemMessageWrapper(message);
                    messageWrapper.getMessage().setRoomId(roomId);

                    webSocketService.sendToAllOnline(WSBaseResp.builder()
                            .type(MessageTypeEnum.DRAW.getType())
                            .data(messageWrapper).build());
                    return null;
                }

                // 检查用户是否是当前绘画者，绘画者不能猜词
                if (loginUser.getId().equals(room.getCurrentDrawerId()) || room.getCorrectGuessIds().contains(loginUser.getId())) {
                    MessageWrapper userMessage = request.getMessageWrapper();

                    String contentWord = userMessage.getMessage().getContent()
                            .replace(room.getCurrentWord(), "***");
                    userMessage.getMessage().setContent(contentWord);

                    userMessage.getMessage().setRoomId(roomId);
                    webSocketService.sendToAllOnline(WSBaseResp.builder()
                            .type(MessageTypeEnum.DRAW.getType())
                            .data(userMessage).build(), loginUser.getId());
                    return null;
                }

                // 创建猜词记录
                DrawGuessVO guessVO = new DrawGuessVO();
                guessVO.setUserId(loginUser.getId());
                guessVO.setUserName(loginUser.getUserName());
                guessVO.setUserAvatar(loginUser.getUserAvatar());
                guessVO.setGuessWord(guessWord);
                guessVO.setGuessTime(new Date());

                // 判断猜词是否正确
                boolean isCorrect = guessWord.trim().equalsIgnoreCase(room.getCurrentWord().trim());
                guessVO.setIsCorrect(isCorrect);

                MessageWrapper userMessage = request.getMessageWrapper();
                if (isCorrect) {
                    userMessage.getMessage().setContent("***");
                }
                userMessage.getMessage().setRoomId(roomId);
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.DRAW.getType())
                        .data(userMessage).build(), loginUser.getId());

                // 保存猜词记录
                List<DrawGuessVO> guesses = new ArrayList<>();
                String guessesJson = stringRedisTemplate.opsForValue().get(
                        DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_GUESSES, roomId));
                if (guessesJson != null) {
                    guesses = objectMapper.readValue(guessesJson, new TypeReference<List<DrawGuessVO>>() {
                    });
                }
                guesses.add(guessVO);
                stringRedisTemplate.opsForValue().set(
                        DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_GUESSES, roomId),
                        objectMapper.writeValueAsString(guesses),
                        60,
                        TimeUnit.MINUTES
                );

                // 如果猜中了，记录用户ID并增加积分
                if (isCorrect) {
                    room.getCorrectGuessIds().add(loginUser.getId());

                    // 根据猜中顺序计算积分
                    int correctGuessCount = room.getCorrectGuessIds().size();
                    int score = 0;

                    // 第一个猜出5分，第二个4分，第三个3分，第四个及之后都是2分，最后一名1分
                    if (correctGuessCount == 1) {
                        score = 5;
                    } else if (correctGuessCount == 2) {
                        score = 4;
                    } else if (correctGuessCount == 3) {
                        score = 3;
                    } else {
                        score = 2;
                    }

                    // 如果是最后一个人猜中（所有人都猜中了），给1分
                    // -1是因为排除绘画者
                    if (correctGuessCount == room.getParticipantIds().size() - 1) {
                        score = 1;
                    }

                    // 从Redis中获取玩家积分信息
                    String playerScoreKey = DrawGameRedisKey.getKey(DrawGameRedisKey.PLAYER_SCORE, roomId, loginUser.getId().toString());
                    String scoreStr = stringRedisTemplate.opsForValue().get(playerScoreKey);
                    int currentScore = 0;
                    if (scoreStr != null) {
                        currentScore = Integer.parseInt(scoreStr);
                    }
                    // 更新积分
                    currentScore += score;
                    // 保存更新后的积分
                    stringRedisTemplate.opsForValue().set(playerScoreKey, String.valueOf(currentScore), 60, TimeUnit.MINUTES);

                    // 更新房间信息
                    String updatedRoomJson = objectMapper.writeValueAsString(room);
                    stringRedisTemplate.opsForValue().set(
                            DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_INFO, roomId),
                            updatedRoomJson,
                            60,
                            TimeUnit.MINUTES
                    );

                    // 发送猜中消息
                    MessageWrapper messageWrapper = getSystemMessageWrapper("恭喜 " + loginUser.getUserName() + " 猜中了词语，获得 " + score + " 分！");
                    messageWrapper.getMessage().setRoomId(roomId);

                    webSocketService.sendToAllOnline(WSBaseResp.builder()
                            .type(MessageTypeEnum.DRAW.getType())
                            .data(messageWrapper).build());

                    // 如果是第一个猜中的人，给绘画者加2分
                    if (correctGuessCount == 1) {
                        // 获取绘画者积分
                        String drawerScoreKey = DrawGameRedisKey.getKey(DrawGameRedisKey.PLAYER_SCORE, roomId, room.getCurrentDrawerId().toString());
                        String drawerScoreStr = stringRedisTemplate.opsForValue().get(drawerScoreKey);
                        int drawerScore = 0;
                        if (drawerScoreStr != null) {
                            drawerScore = Integer.parseInt(drawerScoreStr);
                        }
                        // 绘画者加2分
                        drawerScore += 2;
                        // 保存更新后的积分
                        stringRedisTemplate.opsForValue().set(drawerScoreKey, String.valueOf(drawerScore), 60, TimeUnit.MINUTES);

                        // 获取绘画者信息
                        User drawer = userService.getById(room.getCurrentDrawerId());
                        String drawerName = drawer != null ? drawer.getUserName() : "绘画者";

                        // 发送绘画者加分消息
                        MessageWrapper drawerScoreMessage = getSystemMessageWrapper(drawerName + " 作为绘画者获得 2 分！");
                        drawerScoreMessage.getMessage().setRoomId(roomId);

                        webSocketService.sendToAllOnline(WSBaseResp.builder()
                                .type(MessageTypeEnum.DRAW.getType())
                                .data(drawerScoreMessage).build());
                    }

                } else {
                    // 猜错了，发送提示
                    String message = loginUser.getUserName() + " 猜测「" + guessWord + "」，未猜中";
                    MessageWrapper messageWrapper = getSystemMessageWrapper(message);
                    messageWrapper.getMessage().setRoomId(roomId);

                    webSocketService.sendToAllOnline(WSBaseResp.builder()
                            .type(MessageTypeEnum.DRAW.getType())
                            .data(messageWrapper).build());
                }

                // 通知前端刷新房间状态
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.REFRESH_DRAW.getType())
                        .data("").build());

                return guessVO;
            } catch (JsonProcessingException e) {
                log.error("解析房间信息失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "猜词失败");
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
    public List<DrawGuessVO> getRoomGuesses(String roomId) {
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }

        // 获取猜词记录
        String guessesJson = stringRedisTemplate.opsForValue().get(
                DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_GUESSES, roomId));
        if (guessesJson == null) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(guessesJson, new TypeReference<List<DrawGuessVO>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("解析猜词记录失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取猜词记录失败");
        }
    }

    @Override
    public boolean startGame(String roomId) {
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }

        // 验证用户登录状态
        User loginUser = userService.getLoginUser();

        // 使用分布式锁确保并发安全
        RLock lock = redissonClient.getLock("draw_room_start_lock:" + roomId);
        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后再试");
            }

            // 获取房间信息
            String roomJson = stringRedisTemplate.opsForValue().get(
                    DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_INFO, roomId));
            if (roomJson == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "房间不存在");
            }

            try {
                DrawRoom room = objectMapper.readValue(roomJson, DrawRoom.class);

                // 检查是否为房主
                if (!loginUser.getId().equals(room.getCreatorId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有房主或管理员可以开始游戏");
                }

                // 检查房间状态
                if (room.getStatus() != RoomStatusEnum.WAITING) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "房间已结束无法观战");
                }

                // 检查参与者数量
                if (room.getParticipantIds().size() < 2) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "参与者数量不足，至少需要2人");
                }

                // 更新房间状态
                room.setStatus(RoomStatusEnum.PLAYING);
                room.setStartTime(new Date());
                room.setRoundEndTime(System.currentTimeMillis() / 1000 + room.getRoundDuration());
                room.setCorrectGuessIds(new HashSet<>());
                room.setCurrentRound(1);

                // 根据房间模式设置绘画者
                if (Boolean.TRUE.equals(room.getCreatorOnlyMode())) {
                    // 房主绘画模式，绘画者始终为房主
                    room.setCurrentDrawerId(room.getCreatorId());
                } else {
                    // 轮换模式，如果当前绘画者为空，默认设置为房主
                    if (room.getCurrentDrawerId() == null) {
                        room.setCurrentDrawerId(room.getCreatorId());
                    }
                }

                // 更新房间信息
                String updatedRoomJson = objectMapper.writeValueAsString(room);
                stringRedisTemplate.opsForValue().set(
                        DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_INFO, roomId),
                        updatedRoomJson,
                        60,
                        TimeUnit.MINUTES
                );

                // 清空绘画数据
                stringRedisTemplate.delete(DrawGameRedisKey.getKey(DrawGameRedisKey.DRAW_DATA, roomId));

                // 清空猜词记录
                stringRedisTemplate.delete(DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_GUESSES, roomId));

                // 发送清空画板通知
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.CLEAR_DRAW.getType())
                        .data(roomId).build());

                // 发送游戏开始消息
                MessageWrapper messageWrapper = getSystemMessageWrapper("你画我猜游戏开始啦！房主需要根据提示词进行绘画，其他玩家猜词。提示类别：" + room.getWordHint());
                messageWrapper.getMessage().setRoomId(roomId);

                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.DRAW.getType())
                        .data(messageWrapper).build());

                // 向绘画者发送提示词消息
                User drawer = userService.getById(room.getCurrentDrawerId());
                String drawerName = drawer != null ? drawer.getUserName() : "绘画者";
                MessageWrapper wordMessage = getSystemMessageWrapper("本轮的提示词是「" + room.getCurrentWord() + "」，类别是「" + room.getWordHint() + "」，" + drawerName + "请开始绘画！");
                webSocketService.sendToUid(WSBaseResp.builder()
                        .type(MessageTypeEnum.INFO.getType())
                        .data(wordMessage)
                        .build(), room.getCurrentDrawerId());

                MessageWrapper nextRoundMessage = getSystemMessageWrapper("第 " + 1 + "/" + room.getTotalRounds() + " 轮开始！" + drawerName + " 将进行绘画，提示类别：" + room.getWordHint());
                nextRoundMessage.getMessage().setRoomId(roomId);

                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.DRAW.getType())
                        .data(nextRoundMessage).build());

                // 发送当前绘画者提示
                MessageWrapper drawerInfoMessage = getSystemMessageWrapper("当前绘画者是：" + drawerName);
                drawerInfoMessage.getMessage().setRoomId(roomId);
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.DRAW.getType())
                        .data(drawerInfoMessage).build());

                // 通知前端刷新房间状态
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.REFRESH_DRAW.getType())
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

    @Override
    public boolean endGame(String roomId) {
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }

        // 验证用户登录状态
        User loginUser = userService.getLoginUser();

        // 使用分布式锁确保并发安全
        RLock lock = redissonClient.getLock("draw_room_end_lock:" + roomId);
        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后再试");
            }

            // 获取房间信息
            String roomJson = stringRedisTemplate.opsForValue().get(
                    DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_INFO, roomId));
            if (roomJson == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "房间不存在");
            }

            try {
                DrawRoom room = objectMapper.readValue(roomJson, DrawRoom.class);

                // 检查是否为房主或管理员
                boolean isCreator = loginUser.getId().equals(room.getCreatorId());
                boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());

                if (!isCreator && !isAdmin) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有房主或管理员可以结束游戏");
                }

                // 检查房间状态
                if (room.getStatus() != RoomStatusEnum.PLAYING) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "游戏未开始或已结束");
                }

                // 更新房间状态为已结束
                room.setStatus(RoomStatusEnum.ENDED);

                // 统计游戏结果（可以在这里实现积分计算等逻辑）

                // 更新房间信息
                String updatedRoomJson = objectMapper.writeValueAsString(room);
                stringRedisTemplate.opsForValue().set(
                        DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_INFO, roomId),
                        updatedRoomJson,
                        60,
                        TimeUnit.MINUTES
                );

                // 发送清空画板通知
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.CLEAR_DRAW.getType())
                        .data(roomId).build());

                // 发送游戏结束消息
                MessageWrapper messageWrapper = getSystemMessageWrapper("你画我猜游戏结束！本轮的提示词是「" + room.getCurrentWord() + "」，类别是「" + room.getWordHint() + "」");
                messageWrapper.getMessage().setRoomId(roomId);

                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.DRAW.getType())
                        .data(messageWrapper).build());

                // 通知前端刷新房间状态
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.REFRESH_DRAW.getType())
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
    public boolean quitRoom(String roomId) {
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }

        // 验证用户登录状态
        User loginUser = userService.getLoginUser();

        // 使用分布式锁确保并发安全
        RLock lock = redissonClient.getLock("draw_room_quit_lock:" + roomId);
        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后再试");
            }

            // 获取房间信息
            String roomJson = stringRedisTemplate.opsForValue().get(
                    DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_INFO, roomId));
            if (roomJson == null) {
                // 房间不存在，直接删除用户所在房间记录
                stringRedisTemplate.delete(DrawGameRedisKey.getKey(DrawGameRedisKey.PLAYER_ROOM, String.valueOf(loginUser.getId())));
                return true;
            }

            try {
                DrawRoom room = objectMapper.readValue(roomJson, DrawRoom.class);

                // 检查用户是否在房间中
                if (!room.getParticipantIds().contains(loginUser.getId())) {
                    return true;
                }

                // 从参与者列表中移除用户
                room.getParticipantIds().remove(loginUser.getId());

                // 从正确猜词列表中移除用户
                if (room.getCorrectGuessIds() != null) {
                    room.getCorrectGuessIds().remove(loginUser.getId());
                }

                // 如果退出者是当前绘画者，需要选择新的绘画者或结束游戏
                if (loginUser.getId().equals(room.getCurrentDrawerId())) {
                    if (room.getStatus() == RoomStatusEnum.PLAYING) {
                        // 游戏中，如果是房主退出则选择新房主并设为绘画者
                        if (loginUser.getId().equals(room.getCreatorId())) {
                            // 选择一个新的房主（第一个不是当前用户的参与者）
                            if (!room.getParticipantIds().isEmpty()) {
                                Long newCreatorId = room.getParticipantIds().iterator().next();
                                room.setCreatorId(newCreatorId);
                                room.setCurrentDrawerId(newCreatorId);

                                // 通知新房主
                                User newCreator = userService.getById(newCreatorId);
                                String newCreatorName = newCreator != null ? newCreator.getUserName() : "新房主";
                                MessageWrapper creatorChangeMessage = getSystemMessageWrapper("房主退出了游戏，" + newCreatorName + "成为新的房主和绘画者！");
                                creatorChangeMessage.getMessage().setRoomId(roomId);

                                webSocketService.sendToAllOnline(WSBaseResp.builder()
                                        .type(MessageTypeEnum.DRAW.getType())
                                        .data(creatorChangeMessage).build());

                                // 向新绘画者发送提示词
                                MessageWrapper wordMessage = getSystemMessageWrapper("你是新的房主和绘画者！本轮的提示词是「" + room.getCurrentWord() + "」，类别是「" + room.getWordHint() + "」");
                                webSocketService.sendToUid(WSBaseResp.builder()
                                        .type(MessageTypeEnum.INFO.getType())
                                        .data(wordMessage)
                                        .build(), newCreatorId);
                            } else {
                                // 如果没有其他玩家，则结束游戏
                                room.setStatus(RoomStatusEnum.ENDED);
                                MessageWrapper endMessage = getSystemMessageWrapper("房主退出了游戏，游戏结束！");
                                endMessage.getMessage().setRoomId(roomId);

                                webSocketService.sendToAllOnline(WSBaseResp.builder()
                                        .type(MessageTypeEnum.DRAW.getType())
                                        .data(endMessage).build());
                            }
                        }
                    }
                }

                // 如果房主退出且还有其他人，且游戏不在进行中，则选择新的房主
                if (loginUser.getId().equals(room.getCreatorId()) && !room.getParticipantIds().isEmpty() && room.getStatus() != RoomStatusEnum.PLAYING) {
                    // 选择一个新的房主（第一个不是当前用户的参与者）
                    Long newCreatorId = room.getParticipantIds().iterator().next();
                    room.setCreatorId(newCreatorId);

                    // 通知新房主
                    User newCreator = userService.getById(newCreatorId);
                    String newCreatorName = newCreator != null ? newCreator.getUserName() : "新房主";
                    MessageWrapper creatorChangeMessage = getSystemMessageWrapper("房主退出了游戏，" + newCreatorName + "成为新的房主！");
                    creatorChangeMessage.getMessage().setRoomId(roomId);

                    webSocketService.sendToAllOnline(WSBaseResp.builder()
                            .type(MessageTypeEnum.DRAW.getType())
                            .data(creatorChangeMessage).build());
                }

                // 如果房间没有人了，删除房间
                if (room.getParticipantIds().isEmpty()) {
                    stringRedisTemplate.delete(DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_INFO, roomId));
                    stringRedisTemplate.delete(DrawGameRedisKey.getKey(DrawGameRedisKey.DRAW_DATA, roomId));
                    stringRedisTemplate.delete(DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_GUESSES, roomId));

                    // 更新轻量级房间列表缓存
                    updateRoomListCache();
                } else {
                    // 更新房间信息
                    String updatedRoomJson = objectMapper.writeValueAsString(room);
                    stringRedisTemplate.opsForValue().set(
                            DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_INFO, roomId),
                            updatedRoomJson,
                            60,
                            TimeUnit.MINUTES
                    );
                }

                // 删除用户所在房间记录
                stringRedisTemplate.delete(DrawGameRedisKey.getKey(DrawGameRedisKey.PLAYER_ROOM, String.valueOf(loginUser.getId())));

                // 更新轻量级房间列表缓存
                updateRoomListCache();

                // 发送退出消息
                MessageWrapper quitMessage = getSystemMessageWrapper(loginUser.getUserName() + "退出了你画我猜游戏房间");
                quitMessage.getMessage().setRoomId(roomId);

                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.DRAW.getType())
                        .data(quitMessage).build());

                // 通知前端刷新房间状态
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.REFRESH_DRAW.getType())
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

    @Override
    public boolean removeRoom(String roomId) {
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }

        // 验证是否为管理员
        User loginUser = userService.getLoginUser();
        if (!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有管理员可以移除房间");
        }

        // 使用分布式锁确保操作的原子性
        RLock lock = redissonClient.getLock("draw_room_remove_lock:" + roomId);
        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后再试");
            }

            // 获取房间信息
            String roomJson = stringRedisTemplate.opsForValue().get(
                    DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_INFO, roomId));
            if (roomJson == null) {
                return true;
            }

            try {
                DrawRoom room = objectMapper.readValue(roomJson, DrawRoom.class);

                // 通知所有玩家房间被移除
                if (room.getStatus() == RoomStatusEnum.PLAYING) {
                    MessageWrapper removeMessage = getSystemMessageWrapper("管理员强制移除了游戏房间！");
                    removeMessage.getMessage().setRoomId(roomId);

                    webSocketService.sendToAllOnline(WSBaseResp.builder()
                            .type(MessageTypeEnum.DRAW.getType())
                            .data(removeMessage).build());
                }

                // 删除房间相关的所有信息
                stringRedisTemplate.delete(DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_INFO, roomId));
                stringRedisTemplate.delete(DrawGameRedisKey.getKey(DrawGameRedisKey.DRAW_DATA, roomId));
                stringRedisTemplate.delete(DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_GUESSES, roomId));

                // 更新轻量级房间列表缓存
                updateRoomListCache();

                // 删除所有玩家的房间关联信息
                for (Long playerId : room.getParticipantIds()) {
                    stringRedisTemplate.delete(DrawGameRedisKey.getKey(DrawGameRedisKey.PLAYER_ROOM, String.valueOf(playerId)));
                }

                // 通知前端刷新房间列表
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.REFRESH_DRAW.getType())
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
    public boolean nextRound(String roomId) {
        // 验证参数
        if (StringUtils.isBlank(roomId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间ID不能为空");
        }

        // 验证用户登录状态
        User loginUser = userService.getLoginUser();

        // 使用分布式锁确保并发安全
        RLock lock = redissonClient.getLock("draw_room_next_round_lock:" + roomId);
        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后再试");
            }

            // 获取房间信息
            String roomJson = stringRedisTemplate.opsForValue().get(
                    DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_INFO, roomId));
            if (roomJson == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "房间不存在");
            }

            try {
                DrawRoom room = objectMapper.readValue(roomJson, DrawRoom.class);

                // 检查是否为房主或管理员
                boolean isCreator = loginUser.getId().equals(room.getCreatorId());
                boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());

                if (!isCreator && !isAdmin && !loginUser.getId().equals(room.getCurrentDrawerId())) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有房主或管理员可以进入下一轮");
                }

                // 检查房间状态
                if (room.getStatus() != RoomStatusEnum.PLAYING) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "游戏未开始或已结束");
                }

                // 检查是否已经是最后一轮
                Integer currentRound = room.getCurrentRound();
                if (currentRound == null) {
                    currentRound = 1;
                } else {
                    currentRound += 1;
                }

                // 如果已经是最后一轮，则结算游戏
                if (currentRound > room.getTotalRounds()) {
                    // 更新房间状态为已结束
                    room.setStatus(RoomStatusEnum.ENDED);

                    // 更新房间信息
                    String updatedRoomJson = objectMapper.writeValueAsString(room);
                    stringRedisTemplate.opsForValue().set(
                            DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_INFO, roomId),
                            updatedRoomJson,
                            60,
                            TimeUnit.MINUTES
                    );

                    // 获取所有玩家积分信息
                    StringBuilder scoreInfo = new StringBuilder("游戏结束！最终积分排名：\n");
                    List<DrawPlayerVO> players = new ArrayList<>();

                    for (Long playerId : room.getParticipantIds()) {
                        User player = userService.getById(playerId);
                        if (player != null) {
                            DrawPlayerVO playerVO = new DrawPlayerVO();
                            playerVO.setUserId(playerId);
                            playerVO.setUserName(player.getUserName());

                            // 获取玩家积分
                            String playerScoreKey = DrawGameRedisKey.getKey(DrawGameRedisKey.PLAYER_SCORE, roomId, playerId.toString());
                            String scoreStr = stringRedisTemplate.opsForValue().get(playerScoreKey);
                            int score = 0;
                            if (scoreStr != null) {
                                score = Integer.parseInt(scoreStr);
                            }
                            playerVO.setScore(score);
                            players.add(playerVO);
                        }
                    }

                    // 按积分降序排序
                    players.sort((p1, p2) -> p2.getScore() - p1.getScore());

                    // 构建积分排名信息
                    for (int i = 0; i < players.size(); i++) {
                        DrawPlayerVO player = players.get(i);
                        scoreInfo.append(i + 1).append(". ").append(player.getUserName())
                                .append("：").append(player.getScore()).append("分\n");
                    }

                    // 发送游戏结束消息
                    MessageWrapper messageWrapper = getSystemMessageWrapper(scoreInfo.toString());
                    messageWrapper.getMessage().setRoomId(roomId);

                    webSocketService.sendToAllOnline(WSBaseResp.builder()
                            .type(MessageTypeEnum.DRAW.getType())
                            .data(messageWrapper).build());

                    // 通知前端刷新房间状态
                    webSocketService.sendToAllOnline(WSBaseResp.builder()
                            .type(MessageTypeEnum.REFRESH_DRAW.getType())
                            .data("").build());

                    return true;
                }

                // 进入下一轮
                room.setCurrentRound(currentRound);

                // 重置正确猜词列表
                room.setCorrectGuessIds(new HashSet<>());

                // 选择下一个绘画者，根据房间模式决定
                Long nextDrawerId;

                // 如果是房主绘画模式，绘画者始终为房主
                if (Boolean.TRUE.equals(room.getCreatorOnlyMode())) {
                    nextDrawerId = room.getCreatorId();
                } else {
                    // 轮换模式，按顺序选择下一个绘画者
                    List<Long> participantList = new ArrayList<>(room.getParticipantIds());
                    // 按用户ID排序，保证顺序一致
                    Collections.sort(participantList);

                    // 找到当前绘画者在列表中的位置
                    int currentIndex = participantList.indexOf(room.getCurrentDrawerId());
                    // 选择下一个绘画者（如果当前是最后一个，则选择第一个）
                    int nextIndex = (currentIndex + 1) % participantList.size();
                    nextDrawerId = participantList.get(nextIndex);
                }

                // 如果没有找到下一个绘画者（可能只有一个玩家），则使用当前绘画者
                if (nextDrawerId == null) {
                    nextDrawerId = room.getCurrentDrawerId();
                }
                room.setCurrentDrawerId(nextDrawerId);

                // 选择新的词语
                try {
                    Map<String, String> wordData = getRandomWordWithHint(room.getWordType());
                    room.setCurrentWord(wordData.get("word"));
                    room.setWordHint(wordData.get("hint"));
                } catch (IOException e) {
                    log.error("读取词语文件失败", e);
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取词语文件失败");
                }

                // 设置轮次结束时间
                room.setRoundEndTime(System.currentTimeMillis() / 1000 + room.getRoundDuration());

                // 更新房间信息
                String updatedRoomJson = objectMapper.writeValueAsString(room);
                stringRedisTemplate.opsForValue().set(
                        DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_INFO, roomId),
                        updatedRoomJson,
                        60,
                        TimeUnit.MINUTES
                );

                // 清空绘画数据
                stringRedisTemplate.delete(DrawGameRedisKey.getKey(DrawGameRedisKey.DRAW_DATA, roomId));

                // 清空猜词记录
                stringRedisTemplate.delete(DrawGameRedisKey.getKey(DrawGameRedisKey.ROOM_GUESSES, roomId));

                // 发送清空画板通知
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.CLEAR_DRAW.getType())
                        .data(roomId).build());

                // 发送下一轮开始消息
                User drawer = userService.getById(nextDrawerId);
                String drawerName = drawer != null ? drawer.getUserName() : "绘画者";
                MessageWrapper nextRoundMessage = getSystemMessageWrapper("第 " + currentRound + "/" + room.getTotalRounds() + " 轮开始！" + drawerName + " 将进行绘画，提示类别：" + room.getWordHint());
                nextRoundMessage.getMessage().setRoomId(roomId);

                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.DRAW.getType())
                        .data(nextRoundMessage).build());

                // 向绘画者发送提示词消息
                MessageWrapper wordMessage = getSystemMessageWrapper("本轮的提示词是「" + room.getCurrentWord() + "」，类别是「" + room.getWordHint() + "」，请开始绘画！");
                webSocketService.sendToUid(WSBaseResp.builder()
                        .type(MessageTypeEnum.INFO.getType())
                        .data(wordMessage)
                        .build(), nextDrawerId);

                // 发送当前绘画者提示
                MessageWrapper drawerInfoMessage = getSystemMessageWrapper("当前绘画者是：" + drawerName);
                drawerInfoMessage.getMessage().setRoomId(roomId);
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.DRAW.getType())
                        .data(drawerInfoMessage).build());

                // 通知前端刷新房间状态
                webSocketService.sendToAllOnline(WSBaseResp.builder()
                        .type(MessageTypeEnum.REFRESH_DRAW.getType())
                        .data("").build());

                return true;
            } catch (JsonProcessingException e) {
                log.error("解析房间信息失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "进入下一轮失败");
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
}