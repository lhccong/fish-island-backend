package com.cong.fishisland.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.vote.VoteAddRequest;
import com.cong.fishisland.model.dto.vote.VoteRecordRequest;
import com.cong.fishisland.model.enums.user.PointsRecordSourceEnum;
import com.cong.fishisland.model.vo.vote.VoteOptionVO;
import com.cong.fishisland.model.vo.vote.VoteVO;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.VoteService;
import com.cong.fishisland.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 投票服务实现类
 */
@Service
@Slf4j
public class VoteServiceImpl implements VoteService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserPointsService userPointsService;

    // Redis key 前缀
    private static final String VOTE_KEY_PREFIX = "vote:";
    private static final String VOTE_META_KEY_PREFIX = VOTE_KEY_PREFIX + "meta:";
    private static final String VOTE_COUNT_KEY_PREFIX = VOTE_KEY_PREFIX + "count:";
    private static final String VOTE_USER_KEY_PREFIX = VOTE_KEY_PREFIX + "user:";
    private static final String VOTE_ACTIVE_SET_KEY = VOTE_KEY_PREFIX + "active";

    // 1天过期时间
    private static final Duration EXPIRE_DURATION = Duration.ofDays(1);
    private static final long EXPIRE_SECONDS = 86400;

    // 创建投票所需积分
    private static final int CREATE_VOTE_POINTS = 100;

    @Override
    public String createVote(VoteAddRequest voteAddRequest, Long userId) {
        // 参数校验
        if (voteAddRequest == null || voteAddRequest.getTitle() == null || voteAddRequest.getTitle().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "投票标题不能为空");
        }
        if (voteAddRequest.getOptions() == null || voteAddRequest.getOptions().size() < 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "至少需要2个选项");
        }

        // 扣除积分
        userPointsService.deductPoints(userId, CREATE_VOTE_POINTS, 
                PointsRecordSourceEnum.VOTE_CREATE.getValue(), 
                null, 
                "创建投票");

        // 生成投票ID
        String voteId = IdUtil.fastSimpleUUID();

        // 构建投票元数据
        Map<String, Object> metaData = new HashMap<>(16);
        metaData.put("voteId", voteId);
        metaData.put("title", voteAddRequest.getTitle());
        metaData.put("options", voteAddRequest.getOptions());
        metaData.put("singleChoice", voteAddRequest.getSingleChoice() != null ? voteAddRequest.getSingleChoice() : true);
        metaData.put("creatorId", userId);
        metaData.put("createTime", System.currentTimeMillis());

        // 存储投票元数据到 Redis，设置1天过期
        String metaKey = VOTE_META_KEY_PREFIX + voteId;
        RedisUtils.set(metaKey, JSONUtil.toJsonStr(metaData), EXPIRE_DURATION);

        // 初始化各选项的计数器
        for (int i = 0; i < voteAddRequest.getOptions().size(); i++) {
            String countKey = VOTE_COUNT_KEY_PREFIX + voteId + ":" + i;
            RedisUtils.set(countKey, "0", EXPIRE_DURATION);
        }

        // 添加到活跃投票集合
        RedisUtils.zAdd(VOTE_ACTIVE_SET_KEY, voteId, System.currentTimeMillis());

        log.info("用户 {} 创建投票 {}，标题: {}", userId, voteId, voteAddRequest.getTitle());
        return voteId;
    }

    @Override
    public void vote(VoteRecordRequest voteRecordRequest, Long userId) {
        // 参数校验
        if (voteRecordRequest == null || voteRecordRequest.getVoteId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "投票ID不能为空");
        }
        if (voteRecordRequest.getOptionIndexes() == null || voteRecordRequest.getOptionIndexes().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择投票选项");
        }

        String voteId = voteRecordRequest.getVoteId();

        // 检查投票是否存在
        String metaKey = VOTE_META_KEY_PREFIX + voteId;
        String metaJson = RedisUtils.get(metaKey);
        if (metaJson == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "投票不存在或已过期");
        }

        // 解析元数据
        Map metaData = JSONUtil.toBean(metaJson, Map.class);
        List<String> options = (List<String>) metaData.get("options");
        Boolean singleChoice = (Boolean) metaData.get("singleChoice");

        // 检查是否单选
        if (Boolean.TRUE.equals(singleChoice) && voteRecordRequest.getOptionIndexes().size() > 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该投票为单选");
        }

        // 验证选项索引是否有效
        for (Integer index : voteRecordRequest.getOptionIndexes()) {
            if (index < 0 || index >= options.size()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "选项索引无效");
            }
        }

        // 检查用户是否已投票
        String userVoteKey = VOTE_USER_KEY_PREFIX + voteId + ":" + userId;
        String existingVote = RedisUtils.get(userVoteKey);
        if (existingVote != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已经投过票了");
        }

        // 记录用户投票（存储用户选择的选项索引）
        String votedOptions = voteRecordRequest.getOptionIndexes().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        RedisUtils.set(userVoteKey, votedOptions, EXPIRE_DURATION);

        // 增加各选项的票数
        for (Integer index : voteRecordRequest.getOptionIndexes()) {
            String countKey = VOTE_COUNT_KEY_PREFIX + voteId + ":" + index;
            stringRedisTemplate.opsForValue().increment(countKey);
            // 确保过期时间
            stringRedisTemplate.expire(countKey, EXPIRE_DURATION);
        }

        log.info("用户 {} 参与投票 {}，选项: {}", userId, voteId, votedOptions);
    }

    @Override
    public VoteVO getVoteResult(String voteId, Long userId) {
        if (voteId == null || voteId.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "投票ID不能为空");
        }

        // 获取投票元数据
        String metaKey = VOTE_META_KEY_PREFIX + voteId;
        String metaJson = RedisUtils.get(metaKey);
        if (metaJson == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "投票不存在或已过期");
        }

        Map metaData = JSONUtil.toBean(metaJson, Map.class);
        String title = (String) metaData.get("title");
        List<String> options = (List<String>) metaData.get("options");
        Boolean singleChoice = (Boolean) metaData.get("singleChoice");
        Long createTime = (Long) metaData.get("createTime");

        // 计算剩余时间
        long remainingSeconds = EXPIRE_SECONDS - (System.currentTimeMillis() - createTime) / 1000;
        if (remainingSeconds < 0) {
            remainingSeconds = 0;
        }

        // 获取各选项票数
        List<VoteOptionVO> optionVOList = new ArrayList<>();
        long totalCount = 0;
        for (int i = 0; i < options.size(); i++) {
            String countKey = VOTE_COUNT_KEY_PREFIX + voteId + ":" + i;
            String countStr = RedisUtils.get(countKey);
            long count = countStr != null ? Long.parseLong(countStr) : 0;
            totalCount += count;

            VoteOptionVO optionVO = new VoteOptionVO();
            optionVO.setIndex(i);
            optionVO.setText(options.get(i));
            optionVO.setCount(count);
            optionVOList.add(optionVO);
        }

        // 计算百分比
        for (VoteOptionVO optionVO : optionVOList) {
            if (totalCount > 0) {
                optionVO.setPercentage(Math.round(optionVO.getCount() * 100.0 / totalCount * 100) / 100.0);
            } else {
                optionVO.setPercentage(0.0);
            }
        }

        // 检查当前用户是否已投票
        Boolean hasVoted = false;
        List<Integer> userVotedOptions = new ArrayList<>();
        if (userId != null) {
            String userVoteKey = VOTE_USER_KEY_PREFIX + voteId + ":" + userId;
            String userVoteStr = RedisUtils.get(userVoteKey);
            if (userVoteStr != null) {
                hasVoted = true;
                userVotedOptions = Arrays.stream(userVoteStr.split(","))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
            }
        }

        // 构建返回结果
        VoteVO voteVO = new VoteVO();
        voteVO.setVoteId(voteId);
        voteVO.setTitle(title);
        voteVO.setSingleChoice(singleChoice);
        voteVO.setTotalCount(totalCount);
        voteVO.setOptions(optionVOList);
        voteVO.setHasVoted(hasVoted);
        voteVO.setUserVotedOptions(userVotedOptions);
        voteVO.setRemainingSeconds(remainingSeconds);

        return voteVO;
    }

    @Override
    public List<String> getActiveVoteIds() {
        // 获取活跃投票列表（按创建时间倒序）
        Set<String> voteIds = stringRedisTemplate.opsForZSet()
                .reverseRange(VOTE_ACTIVE_SET_KEY, 0, 99);
        
        if (voteIds == null) {
            return new ArrayList<>();
        }

        // 过滤掉已过期的投票
        return voteIds.stream()
                .filter(voteId -> {
                    String metaKey = VOTE_META_KEY_PREFIX + voteId;
                    return RedisUtils.hasKey(metaKey);
                })
                .collect(Collectors.toList());
    }

    @Override
    public void deleteVote(String voteId, Long userId) {
        if (voteId == null || voteId.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "投票ID不能为空");
        }

        // 获取投票元数据
        String metaKey = VOTE_META_KEY_PREFIX + voteId;
        String metaJson = RedisUtils.get(metaKey);
        if (metaJson == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "投票不存在或已过期");
        }

        Map metaData = JSONUtil.toBean(metaJson, Map.class);
        Long creatorId = (Long) metaData.get("creatorId");

        // 只有创建者或管理员可以删除（简化处理，实际可扩展）
        if (!userId.equals(creatorId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有创建者可以删除投票");
        }

        // 删除相关 Redis 数据
        RedisUtils.delete(metaKey);
        
        List<String> options = (List<String>) metaData.get("options");
        for (int i = 0; i < options.size(); i++) {
            String countKey = VOTE_COUNT_KEY_PREFIX + voteId + ":" + i;
            RedisUtils.delete(countKey);
        }

        // 从活跃集合中移除
        RedisUtils.zRemove(VOTE_ACTIVE_SET_KEY, voteId);

        log.info("用户 {} 删除投票 {}", userId, voteId);
    }
}
