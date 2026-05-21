package com.cong.fishisland.service.impl.luckybag;

import com.alibaba.excel.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.luckybag.CreateLuckyBagRequest;
import com.cong.fishisland.model.entity.chat.RoomMessage;
import com.cong.fishisland.model.entity.donation.DonationRecords;
import com.cong.fishisland.model.entity.luckybag.LuckyBag;
import com.cong.fishisland.model.entity.luckybag.LuckyBagRecord;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.model.enums.MessageTypeEnum;
import com.cong.fishisland.model.enums.UserRoleEnum;
import com.cong.fishisland.model.vo.luckybag.LuckyBagRecordVO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.cong.fishisland.model.enums.user.PointsRecordSourceEnum.*;

/**
 * 福袋服务：用户发送福袋，其他人参与，到期随机开奖并推送聊天室
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LuckyBagServiceImpl implements LuckyBagService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;
    private final UserPointsService userPointsService;
    private final UserVipService userVipService;
    private final WebSocketService webSocketService;
    private final RoomMessageService roomMessageService;
    private final DonationRecordsService donationRecordsService;

    /** 赞助榜累计赞助达到该金额，每日首次发福袋免积分 */
    private static final BigDecimal DONATION_FREE_LUCKY_BAG_AMOUNT = new BigDecimal("100");

    private static final String LUCKY_BAG_KEY_PREFIX = "luckybag:";
    private static final String LUCKY_BAG_PARTICIPANTS_KEY_PREFIX = "luckybag:participants:";
    private static final String LUCKY_BAG_WINNERS_KEY_PREFIX = "luckybag:winners:";
    private static final String LUCKY_BAG_EXPIRE_QUEUE_KEY = "luckybag:expire:queue";
    private static final String LUCKY_BAG_ACTIVE_SET_KEY = "luckybag:active";
    private static final String LUCKY_BAG_DRAW_LOCK_KEY_PREFIX = "luckybag:draw:lock:";
    private static final String LUCKY_BAG_DAILY_COUNT_KEY_PREFIX = "luckybag:daily_count:";

    private static final int DEFAULT_DURATION_SECONDS = 180;
    private static final int MIN_DURATION_SECONDS = 60;
    private static final int MAX_DURATION_SECONDS = 1800;
    private static final int NORMAL_USER_DAILY_LIMIT = 2;
    private static final int VIP_USER_DAILY_LIMIT = 5;
    private static final int ADMIN_DAILY_LIMIT = 10;
    /** 单个中奖用户最多获得的积分 */
    private static final int MAX_AMOUNT_PER_WINNER = 50;
    private static final long REDIS_TTL_BUFFER_SECONDS = 3600;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createLuckyBag(CreateLuckyBagRequest request) {
        User loginUser = userService.getLoginUser();
        boolean freeThisTime = validateCreateRequest(request, loginUser);

        int durationSeconds = resolveDurationSeconds(request.getDurationSeconds());
        String luckyBagId = generateLuckyBagId();
        Date now = new Date();
        Date expireTime = new Date(now.getTime() + durationSeconds * 1000L);

        LuckyBag luckyBag = new LuckyBag();
        luckyBag.setId(luckyBagId);
        luckyBag.setName(request.getName() != null && StringUtils.isNotBlank(request.getName().trim())
                ? request.getName().trim() : "福袋");
        luckyBag.setCreatorId(loginUser.getId());
        luckyBag.setTotalAmount(request.getTotalAmount());
        luckyBag.setWinnerCount(request.getWinnerCount());
        luckyBag.setType(request.getType());
        luckyBag.setDurationSeconds(durationSeconds);
        luckyBag.setCreateTime(now);
        luckyBag.setExpireTime(expireTime);
        luckyBag.setStatus(0);
        luckyBag.setParticipantCount(0);

        long ttlSeconds = durationSeconds + REDIS_TTL_BUFFER_SECONDS;
        String luckyBagKey = LUCKY_BAG_KEY_PREFIX + luckyBagId;
        redisTemplate.opsForValue().set(luckyBagKey, luckyBag, Duration.ofSeconds(ttlSeconds));

        String participantsKey = LUCKY_BAG_PARTICIPANTS_KEY_PREFIX + luckyBagId;
        redisTemplate.opsForSet().add(participantsKey, new HashSet<>());
        redisTemplate.expire(participantsKey, Duration.ofSeconds(ttlSeconds));

        redisTemplate.opsForZSet().add(LUCKY_BAG_EXPIRE_QUEUE_KEY, luckyBagId, expireTime.getTime());
        redisTemplate.opsForZSet().add(LUCKY_BAG_ACTIVE_SET_KEY, luckyBagId, expireTime.getTime());

        if (!freeThisTime) {
            userPointsService.updateUsedPoints(loginUser.getId(), request.getTotalAmount(),
                    LUCKY_BAG_SEND.getValue(), luckyBagId, "发送福袋");
        }

        String dailyCountKey = LUCKY_BAG_DAILY_COUNT_KEY_PREFIX + loginUser.getId() + ":" + getTodayDate();
        Integer dailyCount = (Integer) redisTemplate.opsForValue().get(dailyCountKey);
        redisTemplate.opsForValue().set(dailyCountKey, (dailyCount == null ? 0 : dailyCount) + 1, Duration.ofDays(1));

        String creatorName = loginUser.getUserName() != null ? loginUser.getUserName() : "用户";
        String broadcastContent = creatorName + "创建了一个福袋快来参加吧[luckybag]" + luckyBagId + "[/luckybag]";
        broadcastLuckyBagMessage(broadcastContent, -1L);
        log.info("用户 {} 创建福袋 {}，{}秒后开奖", loginUser.getId(), luckyBagId, durationSeconds);
        return luckyBagId;
    }

    @Override
    public void joinLuckyBag(String luckyBagId, Long userId) {
        LuckyBag luckyBag = getLuckyBagFromRedis(luckyBagId);
        if (luckyBag == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "福袋不存在");
        }
        if (luckyBag.getStatus() != 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "福袋已结束");
        }
        if (luckyBag.getExpireTime() != null && luckyBag.getExpireTime().before(new Date())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "福袋已到期");
        }

        String participantsKey = LUCKY_BAG_PARTICIPANTS_KEY_PREFIX + luckyBagId;
        Long added = redisTemplate.opsForSet().add(participantsKey, userId.toString());
        if (added == null || added == 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已参与该福袋");
        }

        Long size = redisTemplate.opsForSet().size(participantsKey);
        luckyBag.setParticipantCount(size != null ? size.intValue() : 0);
        refreshLuckyBagCache(luckyBag);
    }

    @Override
    public LuckyBag getLuckyBagDetail(String luckyBagId) {
        LuckyBag luckyBag = getLuckyBagFromRedis(luckyBagId);
        if (luckyBag == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "福袋不存在");
        }

        String participantsKey = LUCKY_BAG_PARTICIPANTS_KEY_PREFIX + luckyBagId;
        Long size = redisTemplate.opsForSet().size(participantsKey);
        luckyBag.setParticipantCount(size != null ? size.intValue() : 0);

        User creator = userService.getById(luckyBag.getCreatorId());
        if (creator != null) {
            luckyBag.setCreatorName(creator.getUserName());
            luckyBag.setCreatorAvatar(creator.getUserAvatar());
        }
        return luckyBag;
    }

    @Override
    public List<LuckyBagRecordVO> getLuckyBagWinRecords(String luckyBagId) {
        String winnersKey = LUCKY_BAG_WINNERS_KEY_PREFIX + luckyBagId;
        Set<Object> records = redisTemplate.opsForSet().members(winnersKey);
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream()
                .map(record -> toRecordVO(JSON.parseObject(JSON.toJSONString(record), LuckyBagRecord.class)))
                .sorted(Comparator.comparing(LuckyBagRecordVO::getAmount, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<LuckyBag> getActiveLuckyBags() {
        long now = System.currentTimeMillis();
        Set<Object> ids = redisTemplate.opsForZSet()
                .rangeByScore(LUCKY_BAG_ACTIVE_SET_KEY, now, Double.MAX_VALUE);
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<LuckyBag> result = new ArrayList<>();
        Date nowDate = new Date();
        for (Object idObj : ids) {
            String luckyBagId = idObj.toString();
            LuckyBag luckyBag = getLuckyBagFromRedis(luckyBagId);
            if (luckyBag == null || luckyBag.getStatus() != 0
                    || luckyBag.getExpireTime() == null || !luckyBag.getExpireTime().after(nowDate)) {
                redisTemplate.opsForZSet().remove(LUCKY_BAG_ACTIVE_SET_KEY, luckyBagId);
                continue;
            }
            String participantsKey = LUCKY_BAG_PARTICIPANTS_KEY_PREFIX + luckyBagId;
            Long size = redisTemplate.opsForSet().size(participantsKey);
            luckyBag.setParticipantCount(size != null ? size.intValue() : 0);
            User creator = userService.getById(luckyBag.getCreatorId());
            if (creator != null) {
                luckyBag.setCreatorName(creator.getUserName());
                luckyBag.setCreatorAvatar(creator.getUserAvatar());
            }
            result.add(luckyBag);
        }
        result.sort(Comparator.comparing(LuckyBag::getExpireTime));
        return result;
    }

    @Override
    public void processExpiredLuckyBags() {
        long now = System.currentTimeMillis();
        Set<Object> expiredIds = redisTemplate.opsForZSet()
                .rangeByScore(LUCKY_BAG_EXPIRE_QUEUE_KEY, 0, now);
        if (expiredIds == null || expiredIds.isEmpty()) {
            return;
        }
        for (Object idObj : expiredIds) {
            String luckyBagId = idObj.toString();
            try {
                drawLuckyBag(luckyBagId);
            } catch (Exception e) {
                log.error("福袋 {} 开奖失败", luckyBagId, e);
            } finally {
                redisTemplate.opsForZSet().remove(LUCKY_BAG_EXPIRE_QUEUE_KEY, luckyBagId);
            }
        }
    }

    private void drawLuckyBag(String luckyBagId) {
        String lockKey = LUCKY_BAG_DRAW_LOCK_KEY_PREFIX + luckyBagId;
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofMinutes(10));
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }

        try {
            LuckyBag luckyBag = getLuckyBagFromRedis(luckyBagId);
            if (luckyBag == null || luckyBag.getStatus() != 0) {
                return;
            }

            String participantsKey = LUCKY_BAG_PARTICIPANTS_KEY_PREFIX + luckyBagId;
            Set<Object> participantObjs = redisTemplate.opsForSet().members(participantsKey);
            List<Long> participants = new ArrayList<>();
            if (participantObjs != null) {
                for (Object obj : participantObjs) {
                    participants.add(Long.parseLong(obj.toString()));
                }
            }

            if (participants.isEmpty()) {
                luckyBag.setStatus(2);
                refreshLuckyBagCache(luckyBag);
                userPointsService.updateUsedPoints(luckyBag.getCreatorId(), -luckyBag.getTotalAmount(),
                        LUCKY_BAG_REFUND.getValue(), luckyBagId, "福袋无人参与退回");
                broadcastLuckyBagMessage(
                        String.format("🎁 福袋「%s」已到期，暂无人参与，积分已退回发送者。", luckyBag.getName()),
                        -1L);
                return;
            }

            Collections.shuffle(participants);
            int winnerCount = Math.min(luckyBag.getWinnerCount(), participants.size());
            List<Long> winners = participants.subList(0, winnerCount);
            //分配积分
            SplitPrizeResult splitResult = splitPrize(luckyBag.getTotalAmount(), winnerCount, luckyBag.getType());
            List<Integer> amounts = splitResult.getAmounts();
            if (splitResult.getRefundAmount() > 0) {
                userPointsService.updateUsedPoints(luckyBag.getCreatorId(), -splitResult.getRefundAmount(),
                        LUCKY_BAG_REFUND.getValue(), luckyBagId, "福袋单人积分上限退回");
            }

            String winnersKey = LUCKY_BAG_WINNERS_KEY_PREFIX + luckyBagId;
            List<String> winnerTexts = new ArrayList<>();
            for (int i = 0; i < winners.size(); i++) {
                Long winnerId = winners.get(i);
                int amount = amounts.get(i);

                LuckyBagRecord record = new LuckyBagRecord();
                record.setId(generateRecordId());
                record.setLuckyBagId(luckyBagId);
                record.setUserId(winnerId);
                record.setAmount(amount);
                record.setWinTime(new Date());
                redisTemplate.opsForSet().add(winnersKey, record);

                userPointsService.updateUsedPoints(winnerId, -amount,
                        LUCKY_BAG_WIN.getValue(), luckyBagId, "福袋中奖");

                User user = userService.getById(winnerId);
                String userName = user != null ? user.getUserName() : ("用户" + winnerId);
                winnerTexts.add(userName + "(" + amount + "积分)");
            }

            long ttlSeconds = luckyBag.getDurationSeconds() + REDIS_TTL_BUFFER_SECONDS + 86400;
            redisTemplate.expire(winnersKey, Duration.ofSeconds(ttlSeconds));

            luckyBag.setStatus(1);
            luckyBag.setParticipantCount(participants.size());
            refreshLuckyBagCache(luckyBag);

            String resultMessage = String.format("🎁 福袋「%s」已开奖！恭喜：%s",
                    luckyBag.getName(), String.join("、", winnerTexts));
            broadcastLuckyBagMessage(resultMessage, -1L);
            log.info("福袋 {} 开奖完成，中奖 {} 人", luckyBagId, winners.size());
        } finally {
            redisTemplate.opsForZSet().remove(LUCKY_BAG_ACTIVE_SET_KEY, luckyBagId);
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * @return 本次是否免积分（赞助榜≥100 每日首次）
     */
    private boolean validateCreateRequest(CreateLuckyBagRequest request, User loginUser) {
        if (request.getTotalAmount() == null || request.getWinnerCount() == null || request.getType() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不完整");
        }
        if (request.getTotalAmount() <= 0 || request.getTotalAmount() > 100) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "福袋总积分需在 1-100 之间");
        }
        if (request.getWinnerCount() <= 0 || request.getWinnerCount() > 20) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "中奖人数需在 1-20 之间");
        }
        if (request.getTotalAmount() / request.getWinnerCount() < 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "人均积分不能小于 1");
        }
        int maxPerWinner = (request.getTotalAmount() + request.getWinnerCount() - 1) / request.getWinnerCount();
        if (maxPerWinner > MAX_AMOUNT_PER_WINNER) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "单个中奖用户最多获得" + MAX_AMOUNT_PER_WINNER + "积分，请增加中奖人数或减少总积分");
        }
        if (request.getType() != 1 && request.getType() != 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分配类型无效");
        }

        UserPoints userPoints = userPointsService.getById(loginUser.getId());
        boolean isAdmin = Objects.equals(loginUser.getUserRole(), UserRoleEnum.ADMIN.getValue());
        boolean userVip = userVipService.isUserVip(loginUser.getId());
        if (userPoints.getLevel() < 6 && !isAdmin && !userVip) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您的等级不足，无法发送福袋");
        }

        int dailyLimit = isAdmin ? ADMIN_DAILY_LIMIT : (userVip ? VIP_USER_DAILY_LIMIT : NORMAL_USER_DAILY_LIMIT);
        String dailyCountKey = LUCKY_BAG_DAILY_COUNT_KEY_PREFIX + loginUser.getId() + ":" + getTodayDate();
        Integer dailyCount = (Integer) redisTemplate.opsForValue().get(dailyCountKey);
        int todayCount = dailyCount == null ? 0 : dailyCount;
        if (todayCount >= dailyLimit) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "今日发送福袋次数已达上限");
        }

        int donationFreeCount = getDonationFreeLuckyBagCount(loginUser.getId());
        boolean freeThisTime = donationFreeCount > 0 && todayCount < donationFreeCount;

        if (!freeThisTime && userPoints.getPoints() - userPoints.getUsedPoints() < request.getTotalAmount()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "积分不足");
        }
        return freeThisTime;
    }

    /**
     * 赞助榜累计≥100 时，每日可免积分发福袋 1 次
     */
    private int getDonationFreeLuckyBagCount(Long userId) {
        DonationRecords donationRecords = donationRecordsService.getOne(
                new QueryWrapper<DonationRecords>().eq("userId", userId));
        if (donationRecords != null && donationRecords.getAmount() != null
                && donationRecords.getAmount().compareTo(DONATION_FREE_LUCKY_BAG_AMOUNT) >= 0) {
            return 1;
        }
        return 0;
    }

    private int resolveDurationSeconds(Integer durationSeconds) {
        if (durationSeconds == null) {
            return DEFAULT_DURATION_SECONDS;
        }
        return Math.min(MAX_DURATION_SECONDS, Math.max(MIN_DURATION_SECONDS, durationSeconds));
    }

    private static class SplitPrizeResult {
        private final List<Integer> amounts;
        private final int refundAmount;

        SplitPrizeResult(List<Integer> amounts, int refundAmount) {
            this.amounts = amounts;
            this.refundAmount = refundAmount;
        }

        List<Integer> getAmounts() {
            return amounts;
        }

        int getRefundAmount() {
            return refundAmount;
        }
    }

    /**
     * 将总积分分配给中奖者，单人不超过 {@link #MAX_AMOUNT_PER_WINNER}
     */
    private SplitPrizeResult splitPrize(int totalAmount, int winnerCount, int type) {
        List<Integer> amounts = new ArrayList<>(winnerCount);
        if (type == 2) {
            int each = totalAmount / winnerCount;
            int remainder = totalAmount % winnerCount;
            for (int i = 0; i < winnerCount; i++) {
                amounts.add(Math.min(each + (i < remainder ? 1 : 0), MAX_AMOUNT_PER_WINNER));
            }
            return new SplitPrizeResult(amounts, totalAmount - amounts.stream().mapToInt(Integer::intValue).sum());
        }

        if (winnerCount == 1) {
            int amount = Math.min(totalAmount, MAX_AMOUNT_PER_WINNER);
            amounts.add(amount);
            return new SplitPrizeResult(amounts, totalAmount - amount);
        }

        int remaining = totalAmount;
        Random random = new Random();
        for (int i = 0; i < winnerCount - 1; i++) {
            int leftSlots = winnerCount - i;
            int minAmount = 1;
            int maxAmount = remaining - (leftSlots - 1) * minAmount;
            int avg = remaining / leftSlots;
            maxAmount = Math.min(maxAmount, Math.min(avg * 2, MAX_AMOUNT_PER_WINNER));
            maxAmount = Math.max(minAmount, maxAmount);
            int amount = minAmount + random.nextInt(maxAmount - minAmount + 1);
            amounts.add(amount);
            remaining -= amount;
        }
        int lastAmount = Math.min(remaining, MAX_AMOUNT_PER_WINNER);
        amounts.add(lastAmount);
        remaining -= lastAmount;
        return new SplitPrizeResult(amounts, remaining);
    }

    private LuckyBag getLuckyBagFromRedis(String luckyBagId) {
        Object raw = redisTemplate.opsForValue().get(LUCKY_BAG_KEY_PREFIX + luckyBagId);
        if (raw == null) {
            return null;
        }
        return JSON.parseObject(JSON.toJSONString(raw), LuckyBag.class);
    }

    private void refreshLuckyBagCache(LuckyBag luckyBag) {
        long ttlSeconds = luckyBag.getDurationSeconds() + REDIS_TTL_BUFFER_SECONDS + 86400;
        redisTemplate.opsForValue().set(LUCKY_BAG_KEY_PREFIX + luckyBag.getId(), luckyBag,
                Duration.ofSeconds(ttlSeconds));
    }

    private LuckyBagRecordVO toRecordVO(LuckyBagRecord record) {
        LuckyBagRecordVO vo = new LuckyBagRecordVO();
        BeanUtils.copyProperties(record, vo);
        User user = userService.getById(record.getUserId());
        if (user != null) {
            vo.setUserName(user.getUserName());
            vo.setUserAvatar(user.getUserAvatar());
        }
        return vo;
    }

    private void broadcastLuckyBagMessage(String content, long userId) {
        MessageWrapper messageWrapper = buildSystemMessageWrapper(content);
        messageWrapper.getMessage().setRoomId("-1");
        webSocketService.sendToAllOnline(WSBaseResp.builder()
                .type(MessageTypeEnum.CHAT.getType())
                .data(messageWrapper)
                .build());
        saveMessage(userId, messageWrapper);
    }

    private void saveMessage(long loginUserId, MessageWrapper result) {
        RoomMessage roomMessage = new RoomMessage();
        roomMessage.setUserId(loginUserId);
        roomMessage.setRoomId(-1L);
        roomMessage.setMessageJson(JSON.toJSONString(result));
        roomMessage.setMessageId(result.getMessage().getId());
        roomMessageService.save(roomMessage);
    }

    @NotNull
    private static MessageWrapper buildSystemMessageWrapper(String content) {
        Message message = new Message();
        message.setId(UUID.randomUUID().toString().replace("-", ""));
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

    private String generateLuckyBagId() {
        return "lb" + UUID.randomUUID().toString().replace("-", "");
    }

    private String generateRecordId() {
        return "lbr" + UUID.randomUUID().toString().replace("-", "");
    }

    private String getTodayDate() {
        return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}
