package com.cong.fishisland.service.impl.redpacket;

import com.alibaba.fastjson.JSON;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.redpacket.CreateRedPacketRequest;
import com.cong.fishisland.model.entity.redpacket.RedPacket;
import com.cong.fishisland.model.entity.redpacket.RedPacketRecord;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.model.enums.UserRoleEnum;
import com.cong.fishisland.model.vo.redpacket.RedPacketRecordVO;
import com.cong.fishisland.service.RedPacketService;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
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

    // Redis key前缀
    private static final String RED_PACKET_KEY_PREFIX = "redpacket:";
    private static final String RED_PACKET_RECORD_KEY_PREFIX = "redpacket:record:";
    private static final String RED_PACKET_USER_KEY_PREFIX = "redpacket:user:";
    private static final String RED_PACKET_LOCK_KEY_PREFIX = "redpacket:lock:";
    private static final String RED_PACKET_DAILY_COUNT_KEY_PREFIX = "redpacket:daily_count:";

    // 红包过期时间（24小时）
    private static final long RED_PACKET_EXPIRE_TIME = 24 * 60 * 60;
    // 每日发红包次数限制
    private static final int NORMAL_USER_DAILY_LIMIT = 1;
    private static final int ADMIN_DAILY_LIMIT = 3;

    // 锁的过期时间（10秒）
    private static final long LOCK_EXPIRE_TIME = 10;

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
        if (userPoints.getLevel() < 6 && !Objects.equals(loginUser.getUserRole(), UserRoleEnum.ADMIN.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您的等级不足，无法发送红包");
        }

        // 判断用户是否有足够的积分
        if ((userPoints.getPoints() - userPoints.getUsedPoints() < request.getTotalAmount()) && !Objects.equals(loginUser.getUserRole(), UserRoleEnum.ADMIN.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "积分不足");
        }

        if (request.getTotalAmount() <= 0 || request.getCount() <= 0 || request.getTotalAmount() > 100) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作红包异常,不能发送大额红包");
        }


        // 检查用户每日发红包次数限制
        String dailyCountKey = RED_PACKET_DAILY_COUNT_KEY_PREFIX + loginUser.getId() + ":" + getTodayDate();
        Integer dailyCount = (Integer) redisTemplate.opsForValue().get(dailyCountKey);
        if (dailyCount == null) {
            dailyCount = 0;
        }

        int dailyLimit = Objects.equals(loginUser.getUserRole(), UserRoleEnum.ADMIN.getValue())
                ? ADMIN_DAILY_LIMIT
                : NORMAL_USER_DAILY_LIMIT;

        if (dailyCount >= dailyLimit) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    Objects.equals(loginUser.getUserRole(), UserRoleEnum.ADMIN.getValue())
                            ? "管理员每日最多只能发送3次红包"
                            : "您今日已发送过红包，请明天再来");
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
        if (!Objects.equals(loginUser.getUserRole(), UserRoleEnum.ADMIN.getValue())) {
            userPointsService.updateUsedPoints(loginUser.getId(), request.getTotalAmount());
        }

        // 更新用户每日发红包次数
        redisTemplate.opsForValue().set(dailyCountKey, dailyCount + 1, Duration.ofDays(1));

        return redPacketId;
    }

    @Override
    public Integer grabRedPacket(String redPacketId, Long userId) {
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

        // 获取分布式锁
        String lockKey = RED_PACKET_LOCK_KEY_PREFIX + redPacketId;
        boolean locked = false;
        try {
            // 尝试获取锁，最多重试5次，快速重试
            int retryCount = 0;
            int maxRetries = 5;
            long retryWaitTime = 50; // 只等待50毫秒
            
            while (!locked && retryCount < maxRetries) {
                locked = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_EXPIRE_TIME, TimeUnit.SECONDS));
                if (locked) {
                    break;
                }
                
                // 快速重试，几乎是立即重试
                try {
                    // 仅记录第一次和最后一次重试的日志，减少日志量
                    if (retryCount == 0 || retryCount == maxRetries - 1) {
                        log.info("获取锁失败，正在重试 {}/{}，红包ID: {}", retryCount + 1, maxRetries, redPacketId);
                    }
                    Thread.sleep(retryWaitTime);
                    // 重试时间增长较小，保持快速响应
                    retryWaitTime += 20;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                retryCount++;
            }
            
            if (!locked) {
                log.info("获取锁最终失败，已重试{}次: {}", maxRetries, redPacketId);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "抢红包人数过多，请稍后再试");
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
            userPointsService.updateUsedPoints(userId, -amount);

            return amount;
        } finally {
            // 释放锁
            if (locked) {
                redisTemplate.delete(lockKey);
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
} 