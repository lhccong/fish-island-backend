package com.cong.fishisland.service.impl.redpacket;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.redpacket.CreateRedPacketRequest;

import static com.cong.fishisland.model.enums.user.PointsRecordSourceEnum.*;

import com.cong.fishisland.model.entity.chat.RoomMessage;
import com.cong.fishisland.model.entity.donation.DonationRecords;
import com.cong.fishisland.model.entity.redpacket.RedPacket;
import com.cong.fishisland.model.entity.redpacket.RedPacketRecord;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.model.enums.MessageTypeEnum;
import com.cong.fishisland.model.enums.UserRoleEnum;
import com.cong.fishisland.model.vo.redpacket.RedPacketRecordVO;
import com.cong.fishisland.model.ws.request.Message;
import com.cong.fishisland.model.ws.request.MessageWrapper;
import com.cong.fishisland.model.ws.request.Sender;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import com.cong.fishisland.service.*;
import com.cong.fishisland.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 红包服务实现类
 *
 * @author cong
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RedPacketServiceImpl implements RedPacketService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;
    private final UserPointsService userPointsService;
    private final UserVipService userVipService;
    private final WebSocketService webSocketService;
    private final RoomMessageService roomMessageService;
    private final DonationRecordsService donationRecordsService;
    private final ScriptBehaviorDetectService scriptBehaviorDetectService;

    // Redis key前缀
    private static final String RED_PACKET_KEY_PREFIX = "redpacket:";
    private static final String RED_PACKET_RECORD_KEY_PREFIX = "redpacket:record:";
    private static final String RED_PACKET_USER_KEY_PREFIX = "redpacket:user:";
    private static final String RED_PACKET_DAILY_COUNT_KEY_PREFIX = "redpacket:daily_count:";
    /** 红包与福袋共享免积分次数 */
    private static final String SEND_FREE_COUNT_KEY_PREFIX = "send:free_count:";
    // 行为检测：每日快速抢包计数 redpacket:grab:fast_count:{userId}:{yyyyMMdd}
    private static final String RED_PACKET_GRAB_FAST_COUNT_KEY_PREFIX = "redpacket:grab:fast_count:";
    // 行为检测：抢包时间戳历史 redpacket:grab:ts:{userId}
    private static final String RED_PACKET_GRAB_TS_KEY_PREFIX = "redpacket:grab:ts:";
    // 判定为脚本的阈值：红包发出后多少毫秒内抢到视为脚本
    private static final long GRAB_SCRIPT_THRESHOLD_MS = 1000;
    // 每日触发快速抢包超过此次数才标记为脚本用户
    private static final int GRAB_SCRIPT_DAILY_LIMIT = 5;

    // 红包过期时间（24小时）
    private static final long RED_PACKET_EXPIRE_TIME = 24 * 60 * 60;
    // 每日发红包次数限制
    private static final int NORMAL_USER_DAILY_LIMIT = 1;
    private static final int VIP_USER_DAILY_LIMIT = 3;
    private static final int ADMIN_DAILY_LIMIT = 3;

    // VIP 打赏榜免积分阈值
    private static final BigDecimal VIP_DONATION_FREE_TWO = new BigDecimal("29");
    private static final BigDecimal VIP_DONATION_FREE_ALL = new BigDecimal("100");

    // 每个红包的本地排队信号量，保证同一时刻只有一个线程执行抢红包核心逻辑
    private final ConcurrentHashMap<String, Semaphore> redPacketSemaphores = new ConcurrentHashMap<>();
    // 排队最长等待时间（秒）
    private static final int QUEUE_WAIT_TIMEOUT_SECONDS = 10;

    @Scheduled(cron = "0 0 10,15 * * ?") // 每天上午10点和下午3点各执行一次
    public void aiSendRedPacket() {
        // 生成红包ID
        String redPacketId = generateRedPacketId();

        // 创建红包对象
        RedPacket redPacket = new RedPacket();
        redPacket.setId(redPacketId);
        redPacket.setName("我是小助手我给大家发红包啦");
        redPacket.setCreatorId(-1L);
        redPacket.setTotalAmount(200);
        redPacket.setCount(20);
        redPacket.setType(2);
        redPacket.setRemainingAmount(200);
        redPacket.setRemainingCount(20);
        redPacket.setCreateTime(new Date());
        redPacket.setExpireTime(new Date(System.currentTimeMillis() + RED_PACKET_EXPIRE_TIME * 1000));
        // 进行中
        redPacket.setStatus(0);

        // 如果是平均红包，计算每个红包的金额
        redPacket.setAmountPerPacket(10);

        // 将红包信息存入Redis
        String redPacketKey = RED_PACKET_KEY_PREFIX + redPacketId;
        redisTemplate.opsForValue().set(redPacketKey, redPacket, Duration.ofSeconds(RED_PACKET_EXPIRE_TIME));

        // 创建红包记录集合
        String redPacketRecordKey = RED_PACKET_RECORD_KEY_PREFIX + redPacketId;
        redisTemplate.expire(redPacketRecordKey, Duration.ofSeconds(RED_PACKET_EXPIRE_TIME));

        // 创建红包用户集合（用于记录抢过红包的用户）
        String redPacketUserKey = RED_PACKET_USER_KEY_PREFIX + redPacketId;
        redisTemplate.opsForSet().add(redPacketUserKey, new HashSet<>());
        redisTemplate.expire(redPacketUserKey, Duration.ofSeconds(RED_PACKET_EXPIRE_TIME));

        MessageWrapper systemMessageWrapper = getSystemMessageWrapper("[redpacket]" + redPacketId + "[/redpacket]");
        systemMessageWrapper.getMessage().setRoomId("-1");

        webSocketService.sendToAllOnline(WSBaseResp.builder()
                .type(MessageTypeEnum.CHAT.getType())
                .data(systemMessageWrapper).build());

        saveMessage(-1, systemMessageWrapper);


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
    @Transactional(rollbackFor = Exception.class)
    public String createRedPacket(CreateRedPacketRequest request) {
        // 获取当前登录用户ID
        User loginUser = userService.getLoginUser();
        //红包金额是大于等于红包个数
        if (request.getTotalAmount() / request.getCount() < 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作红包异常,红包个数不能小于红包金额");
        }

        if (request.getTotalAmount() / request.getCount() > 10) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作红包异常,不能发送大额红包");
        }

        //获取当前用户积分
        UserPoints userPoints = userPointsService.getById(loginUser.getId());
        if (userPoints.getLevel() < 6 && !Objects.equals(loginUser.getUserRole(), UserRoleEnum.ADMIN.getValue()) && !userVipService.isUserVip(loginUser.getId())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您的等级不足，无法发送红包");
        }

        // 检查用户每日发红包次数限制
        String dailyCountKey = RED_PACKET_DAILY_COUNT_KEY_PREFIX + loginUser.getId() + ":" + getTodayDate();
        Integer dailyCount = (Integer) redisTemplate.opsForValue().get(dailyCountKey);
        if (dailyCount == null) {
            dailyCount = 0;
        }

        int dailyLimit;
        boolean userVip = userVipService.isUserVip(loginUser.getId());
        if (Objects.equals(loginUser.getUserRole(), UserRoleEnum.ADMIN.getValue())) {
            dailyLimit = ADMIN_DAILY_LIMIT;
        } else if (userVip) {
            dailyLimit = VIP_USER_DAILY_LIMIT;
        } else {
            dailyLimit = NORMAL_USER_DAILY_LIMIT;
        }

        // 计算 VIP 用户今日免积分次数（与福袋共享）
        int vipFreeCount = resolveVipFreeCount(userVip, loginUser.getId());

        // 判断本次是否需要消耗积分
        boolean isAdmin = Objects.equals(loginUser.getUserRole(), UserRoleEnum.ADMIN.getValue());
        int sharedFreeUsedCount = getSharedFreeUsedCount(loginUser.getId());
        boolean freeThisTime = isAdmin || (userVip && sharedFreeUsedCount < vipFreeCount);

        // 判断用户是否有足够的积分（免费次数内不检查）
        if (!freeThisTime && (userPoints.getPoints() - userPoints.getUsedPoints() < request.getTotalAmount())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "积分不足");
        }

        if (request.getTotalAmount() <= 0 || request.getCount() <= 0 || request.getTotalAmount() > 100) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作红包异常,不能发送大额红包");
        }

        if (dailyCount >= dailyLimit) {
            String message;
            if (Objects.equals(loginUser.getUserRole(), UserRoleEnum.ADMIN.getValue())) {
                message = "管理员每日最多只能发送3次红包";
            } else if (userVip) {
                message = "VIP用户每日最多只能发送3次红包";
            } else {
                message = "您今日已发送过红包，请明天再来";
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, message);
        }


        // 生成红包ID
        String redPacketId = generateRedPacketId();

        // 创建红包对象
        RedPacket redPacket = new RedPacket();
        redPacket.setId(redPacketId);
        redPacket.setName(request.getName());
        redPacket.setCreatorId(loginUser.getId());
        redPacket.setTotalAmount(request.getTotalAmount());
        redPacket.setCount(request.getCount());
        redPacket.setType(request.getType());
        redPacket.setRemainingAmount(request.getTotalAmount());
        redPacket.setRemainingCount(request.getCount());
        redPacket.setCreateTime(new Date());
        redPacket.setExpireTime(new Date(System.currentTimeMillis() + RED_PACKET_EXPIRE_TIME * 1000));
        // 进行中
        redPacket.setStatus(0);

        // 如果是平均红包，计算每个红包的金额
        if (request.getType() == 2) {
            redPacket.setAmountPerPacket(request.getTotalAmount() / request.getCount());
        }

        // 将红包信息存入Redis
        String redPacketKey = RED_PACKET_KEY_PREFIX + redPacketId;
        redisTemplate.opsForValue().set(redPacketKey, redPacket, Duration.ofSeconds(RED_PACKET_EXPIRE_TIME));

        // 创建红包记录集合
        String redPacketRecordKey = RED_PACKET_RECORD_KEY_PREFIX + redPacketId;
//        redisTemplate.opsForSet().add(redPacketRecordKey, new HashSet<>());
        redisTemplate.expire(redPacketRecordKey, Duration.ofSeconds(RED_PACKET_EXPIRE_TIME));

        // 创建红包用户集合（用于记录抢过红包的用户）
        String redPacketUserKey = RED_PACKET_USER_KEY_PREFIX + redPacketId;
        redisTemplate.opsForSet().add(redPacketUserKey, new HashSet<>());
        redisTemplate.expire(redPacketUserKey, Duration.ofSeconds(RED_PACKET_EXPIRE_TIME));

        //扣减用户可用积分
        if (!freeThisTime) {
            userPointsService.updateUsedPoints(loginUser.getId(), request.getTotalAmount(), RED_PACKET_SEND.getValue(), redPacketId, "发送红包");
        }

        // 更新用户每日发红包次数
        redisTemplate.opsForValue().set(dailyCountKey, dailyCount + 1, Duration.ofDays(1));
        if (freeThisTime && userVip && !isAdmin) {
            incrementSharedFreeUsedCount(loginUser.getId());
        }

        return redPacketId;
    }

    @Override
    public Integer grabRedPacket(String redPacketId, Long userId) {
        // 脚本用户不允许抢红包，直接返回已抢完
        if (scriptBehaviorDetectService.isScriptUser(userId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "红包已经抢完");
        }

        // 获取红包信息
        String redPacketKey = RED_PACKET_KEY_PREFIX + redPacketId;
        RedPacket redPacket = JSON.parseObject(JSON.toJSONString(redisTemplate.opsForValue().get(redPacketKey)), RedPacket.class);

        if (redPacket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "红包不存在");
        }

        // 检查红包状态
        if (redPacket.getStatus() != 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "红包已抢完或已过期");
        }

        // 检查用户是否已抢过红包
        String redPacketUserKey = RED_PACKET_USER_KEY_PREFIX + redPacketId;
        Boolean isMember = redisTemplate.opsForSet().isMember(redPacketUserKey, userId.toString());
        if (Boolean.TRUE.equals(isMember)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已抢过该红包");
        }

        // 本地信号量排队：同一红包同一时刻只允许一个线程执行核心逻辑，其余线程按先后顺序等待
        Semaphore semaphore = redPacketSemaphores.computeIfAbsent(redPacketId, k -> new Semaphore(1, true));
        boolean acquired = false;
        try {
            // 排队等待信号量，最多等待 QUEUE_WAIT_TIMEOUT_SECONDS 秒
            try {
                acquired = semaphore.tryAcquire(QUEUE_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "抢红包被中断，请重试");
            }
            if (!acquired) {
                log.warn("用户 {} 排队超时，红包ID: {}", userId, redPacketId);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "排队超时，请稍后再试");
            }

            // 再次检查红包状态（双重检查）
            redPacket = JSON.parseObject(JSON.toJSONString(redisTemplate.opsForValue().get(redPacketKey)), RedPacket.class);
            if (redPacket == null || redPacket.getStatus() != 0 || redPacket.getRemainingCount() <= 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "红包已抢完或已过期");
            }

            // 再次检查用户是否已抢过红包（双重检查）
            isMember = redisTemplate.opsForSet().isMember(redPacketUserKey, userId.toString());
            if (Boolean.TRUE.equals(isMember)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已抢过该红包");
            }

            // 计算抢到的金额
            Integer amount;
            if (redPacket.getType() == 1) {
                // 随机红包
                amount = calculateRandomAmount(redPacket);
            } else {
                // 平均红包
                amount = redPacket.getAmountPerPacket();
            }

            // 更新红包信息
            redPacket.setRemainingAmount(redPacket.getRemainingAmount() - amount);
            redPacket.setRemainingCount(redPacket.getRemainingCount() - 1);

            // 如果红包已抢完，更新状态
            if (redPacket.getRemainingCount() <= 0) {
                // 已抢完
                redPacket.setStatus(1);
            }

            // 更新Redis中的红包信息
            redisTemplate.opsForValue().set(redPacketKey, redPacket, Duration.ofSeconds(RED_PACKET_EXPIRE_TIME));

            // 记录用户已抢过该红包
            redisTemplate.opsForSet().add(redPacketUserKey, userId.toString());

            // 创建抢红包记录
            RedPacketRecord record = new RedPacketRecord();
            record.setId(generateRecordId());
            record.setRedPacketId(redPacketId);
            record.setUserId(userId);
            record.setAmount(amount);
            record.setGrabTime(new Date());

            // 将抢红包记录存入Redis
            String redPacketRecordKey = RED_PACKET_RECORD_KEY_PREFIX + redPacketId;
            redisTemplate.opsForSet().add(redPacketRecordKey, record);

            //增加用户积分
            userPointsService.updateUsedPoints(userId, -amount, RED_PACKET_GRAB.getValue(), redPacketId, "抢红包获得积分");

            // 行为检测：若在红包发出后1秒内抢到，标记为脚本用户
            markScriptUserIfNeeded(userId, redPacket.getCreateTime());
            // 行为检测：固定间隔抢包检测
            scriptBehaviorDetectService.checkFixedIntervalBehavior(userId, RED_PACKET_GRAB_TS_KEY_PREFIX, "抢红包");

            return amount;
        } finally {
            // 释放本地信号量，让下一个排队的线程进入
            if (acquired) {
                semaphore.release();
            }
            // 红包已抢完时清理信号量，避免内存泄漏
            RedPacket finalRedPacket = JSON.parseObject(
                    JSON.toJSONString(redisTemplate.opsForValue().get(RED_PACKET_KEY_PREFIX + redPacketId)),
                    RedPacket.class);
            if (finalRedPacket == null || finalRedPacket.getStatus() != 0) {
                redPacketSemaphores.remove(redPacketId);
            }
        }
    }

    @Override
    public List<RedPacketRecordVO> getRedPacketRecords(String redPacketId) {
        // 获取红包记录
        String redPacketRecordKey = RED_PACKET_RECORD_KEY_PREFIX + redPacketId;
        Set<Object> records = redisTemplate.opsForSet().members(redPacketRecordKey);

        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        // 转换为VO列表
        return records.stream()
                .map(record -> {
                    RedPacketRecord redPacketRecord = JSON.parseObject(JSON.toJSONString(record), RedPacketRecord.class);
                    RedPacketRecordVO vo = new RedPacketRecordVO();
                    BeanUtils.copyProperties(redPacketRecord, vo);

                    // 获取用户信息
                    User user = userService.getById(redPacketRecord.getUserId());
                    if (user != null) {
                        vo.setUserName(user.getUserName());
                        vo.setUserAvatar(user.getUserAvatar());
                    }

                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public RedPacket getRedPacketDetail(String redPacketId) {
        // 获取红包信息
        String redPacketKey = RED_PACKET_KEY_PREFIX + redPacketId;
        RedPacket redPacket = JSON.parseObject(JSON.toJSONString(redisTemplate.opsForValue().get(redPacketKey)), RedPacket.class);

        if (redPacket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "红包不存在");
        }

        // 获取红包记录
        String redPacketRecordKey = RED_PACKET_RECORD_KEY_PREFIX + redPacketId;
        Set<Object> records = redisTemplate.opsForSet().members(redPacketRecordKey);

        // 设置已抢红包数量
        if (records != null) {
            redPacket.setGrabCount(records.size());
        }

        // 获取创建者信息
        User creator = userService.getById(redPacket.getCreatorId());
        if (creator != null) {
            redPacket.setCreatorName(creator.getUserName());
            redPacket.setCreatorAvatar(creator.getUserAvatar());
        }

        return redPacket;
    }

    /**
     * 生成红包ID
     */
    private String generateRedPacketId() {
        return "rp" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成记录ID
     */
    private String generateRecordId() {
        return "rpr" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 计算随机红包金额
     */
    private Integer calculateRandomAmount(RedPacket redPacket) {
        // 如果是最后一个红包，直接返回剩余金额
        if (redPacket.getRemainingCount() == 1) {
            return redPacket.getRemainingAmount();
        }

        // 计算平均金额
        int averageAmount = redPacket.getTotalAmount() / redPacket.getCount();
        // 设置最大金额为平均金额的2倍
        int maxAllowedAmount = averageAmount * 2;

        // 随机生成金额，保证每个红包至少有1积分
        int minAmount = 1;
        // 取剩余金额和最大允许金额中的较小值作为上限
        int maxAmount = Math.min(
                redPacket.getRemainingAmount() - (redPacket.getRemainingCount() - 1) * minAmount,
                maxAllowedAmount
        );

        // 使用Random生成随机数
        Random random = new Random();

        return random.nextInt(maxAmount) + minAmount;
    }

    /**
     * 获取今天的日期字符串（格式：yyyyMMdd）
     */
    private String getTodayDate() {
        return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    private int resolveVipFreeCount(boolean userVip, Long userId) {
        if (!userVip) {
            return 0;
        }
        int vipFreeCount = 1;
        DonationRecords donationRecords = donationRecordsService.getOne(
                new QueryWrapper<DonationRecords>().eq("userId", userId));
        if (donationRecords != null && donationRecords.getAmount() != null) {
            BigDecimal donationAmount = donationRecords.getAmount();
            if (donationAmount.compareTo(VIP_DONATION_FREE_ALL) >= 0) {
                vipFreeCount = ADMIN_DAILY_LIMIT;
            } else if (donationAmount.compareTo(VIP_DONATION_FREE_TWO) >= 0) {
                vipFreeCount = 2;
            }
        }
        return vipFreeCount;
    }

    private int getSharedFreeUsedCount(Long userId) {
        String key = SEND_FREE_COUNT_KEY_PREFIX + userId + ":" + getTodayDate();
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        return count == null ? 0 : count;
    }

    private void incrementSharedFreeUsedCount(Long userId) {
        String key = SEND_FREE_COUNT_KEY_PREFIX + userId + ":" + getTodayDate();
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        redisTemplate.opsForValue().set(key, (count == null ? 0 : count) + 1, Duration.ofDays(1));
    }

    /**
     * 抢到红包后判断是否为脚本行为：距红包创建时间不足 1 秒则计数，当天超过 5 次才标记
     */
    private void markScriptUserIfNeeded(Long userId, Date redPacketCreateTime) {
        if (redPacketCreateTime == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - redPacketCreateTime.getTime();
        if (elapsed >= GRAB_SCRIPT_THRESHOLD_MS) {
            return;
        }
        // 累加当日快速抢包次数
        String fastCountKey = RED_PACKET_GRAB_FAST_COUNT_KEY_PREFIX + userId + ":" + getTodayDate();
        Long fastCount = redisTemplate.opsForValue().increment(fastCountKey);
        redisTemplate.expire(fastCountKey, Duration.ofDays(1));

        log.info("用户 {} 在红包发出后 {}ms 内抢到，今日快速抢包次数: {}", userId, elapsed, fastCount);

        if (fastCount != null && fastCount > GRAB_SCRIPT_DAILY_LIMIT) {
            User user = userService.getById(userId);
            scriptBehaviorDetectService.markAsScriptUser(userId,
                    String.format("检测到用户 %s 今日快速抢包次数达到 %d 次，已标记为脚本用户",
                            user.getUserName() + ":" + user.getId(), fastCount));
        }
    }

    @Override
    public void markScriptUser(Long userId, boolean mark) {
        scriptBehaviorDetectService.markScriptUser(userId, mark);
    }

    private void saveMessage(long loginUserId, MessageWrapper result) {
        //保存消息到数据库
        RoomMessage roomMessage = new RoomMessage();
        roomMessage.setUserId(loginUserId);
        roomMessage.setRoomId(-1L);
        roomMessage.setMessageJson(JSON.toJSONString(result));
        roomMessage.setMessageId(result.getMessage().getId());
        roomMessageService.save(roomMessage);
    }
}