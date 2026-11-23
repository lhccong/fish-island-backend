package com.cong.fishisland.service.impl.farm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.farm.FarmAchievementProgressMapper;
import com.cong.fishisland.mapper.farm.FarmAchievementTaskMapper;
import com.cong.fishisland.mapper.farm.FarmCollectionGradeMapper;
import com.cong.fishisland.mapper.farm.FarmUserBuffMapper;
import com.cong.fishisland.mapper.farm.FarmUserMapper;
import com.cong.fishisland.model.dto.farm.FarmAchievementTaskVO;
import com.cong.fishisland.model.entity.farm.FarmAchievementProgress;
import com.cong.fishisland.model.entity.farm.FarmAchievementTask;
import com.cong.fishisland.model.entity.farm.FarmCollectionGrade;
import com.cong.fishisland.model.entity.farm.FarmUser;
import com.cong.fishisland.model.entity.farm.FarmUserBuff;
import com.cong.fishisland.model.enums.farm.FarmAchievementTypeEnum;
import com.cong.fishisland.model.enums.user.PointsRecordSourceEnum;
import com.cong.fishisland.service.FarmAchievementService;
import com.cong.fishisland.service.UserPointsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 农场成就任务服务实现。
 * <p>完成状态由源数据推导（buff 等级/持有、图鉴品级、农场等级），
 * 依赖全部走 Mapper 与积分服务，避免与服务层形成循环依赖。</p>
 */
@Slf4j
@Service
public class FarmAchievementServiceImpl extends ServiceImpl<FarmAchievementTaskMapper, FarmAchievementTask>
        implements FarmAchievementService {

    @Resource
    private FarmAchievementProgressMapper progressMapper;

    @Resource
    private FarmUserBuffMapper buffMapper;

    @Resource
    private FarmCollectionGradeMapper collectionGradeMapper;

    @Resource
    private FarmUserMapper farmUserMapper;

    @Resource
    private UserPointsService userPointsService;

    @Override
    public void onProgress(Long userId, FarmAchievementTypeEnum type, String targetId, Integer value) {
        if (userId == null || type == null) {
            return;
        }
        List<FarmAchievementTask> tasks = list(new LambdaQueryWrapper<FarmAchievementTask>()
                .eq(FarmAchievementTask::getStatus, 1)
                .eq(FarmAchievementTask::getType, type.getValue()));
        for (FarmAchievementTask task : tasks) {
            if (isSatisfied(userId, task)) {
                progressMapper.upsertProgress(userId, task.getId(), 1);
            }
        }
    }

    @Override
    public List<FarmAchievementTaskVO> listMy(Long userId) {
        List<FarmAchievementTask> tasks = list(new LambdaQueryWrapper<FarmAchievementTask>()
                .eq(FarmAchievementTask::getStatus, 1)
                .orderByAsc(FarmAchievementTask::getSortOrder));
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }
        // 刷新一次完成状态，防止事件遗漏
        for (FarmAchievementTask task : tasks) {
            if (isSatisfied(userId, task)) {
                progressMapper.upsertProgress(userId, task.getId(), 1);
            }
        }
        Map<Long, FarmAchievementProgress> progressMap = progressMapper.selectList(
                        new LambdaQueryWrapper<FarmAchievementProgress>()
                                .eq(FarmAchievementProgress::getUserId, userId))
                .stream()
                .collect(Collectors.toMap(FarmAchievementProgress::getTaskId, Function.identity(), (a, b) -> a));

        List<FarmAchievementTaskVO> result = new ArrayList<>(tasks.size());
        for (FarmAchievementTask task : tasks) {
            FarmAchievementProgress progress = progressMap.get(task.getId());
            result.add(new FarmAchievementTaskVO(
                    task.getId(),
                    task.getName(),
                    task.getDescription(),
                    task.getType(),
                    task.getTargetCount(),
                    task.getRewardValue(),
                    progress != null ? progress.getStatus() : 0,
                    progress != null ? progress.getCompletedTime() : null));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claim(Long userId, Long taskId) {
        if (taskId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        }
        FarmAchievementTask task = getById(taskId);
        if (task == null || task.getStatus() == null || task.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "成就任务不存在或未启用");
        }
        FarmAchievementProgress progress = progressMapper.selectOne(
                new LambdaQueryWrapper<FarmAchievementProgress>()
                        .eq(FarmAchievementProgress::getUserId, userId)
                        .eq(FarmAchievementProgress::getTaskId, taskId));
        if (progress == null || progress.getStatus() == null || progress.getStatus() < 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "成就尚未达成，无法领取");
        }
        if (progress.getStatus() == 2) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "奖励已领取，请勿重复领取");
        }
        if (task.getRewardType() == null || task.getRewardType() != 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该成就奖励类型暂未开放领取");
        }
        int reward = task.getRewardValue() != null ? task.getRewardValue() : 0;
        if (reward > 0) {
            userPointsService.updateUsedPoints(userId, -reward,
                    PointsRecordSourceEnum.FARM_ACHIEVEMENT.getValue(),
                    taskId.toString(),
                    "农场成就奖励-" + (task.getName() != null ? task.getName() : taskId));
        }
        int updated = progressMapper.markClaimed(userId, taskId);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "奖励已领取，请勿重复领取");
        }
        log.info("成就奖励领取 userId={}, taskId={}, reward={}", userId, taskId, reward);
    }

    /**
     * 由源数据判断成就是否达成（源数据单调递增，达成即永久成立）。
     */
    private boolean isSatisfied(Long userId, FarmAchievementTask task) {
        FarmAchievementTypeEnum type = FarmAchievementTypeEnum.fromValue(task.getType());
        if (type == null) {
            return false;
        }
        int targetCount = task.getTargetCount() != null ? task.getTargetCount() : 0;
        switch (type) {
            case BUFF_PURCHASE:
                return existsBuff(userId, parseLong(task.getTargetId()), 1);
            case BUFF_UPGRADE:
                return existsBuff(userId, parseLong(task.getTargetId()), targetCount);
            case COLLECTION_UNLOCK:
                return collectionGradeMapper.selectCount(new LambdaQueryWrapper<FarmCollectionGrade>()
                        .eq(FarmCollectionGrade::getUserId, userId)
                        .ge(FarmCollectionGrade::getGrade, targetCount)) > 0;
            case FARM_LEVEL:
                FarmUser farmUser = farmUserMapper.selectById(userId);
                return farmUser != null && farmUser.getLevel() != null && farmUser.getLevel() >= targetCount;
            default:
                return false;
        }
    }

    private boolean existsBuff(Long userId, Long buffType, int minLevel) {
        if (buffType == null) {
            return false;
        }
        return buffMapper.selectCount(new LambdaQueryWrapper<FarmUserBuff>()
                .eq(FarmUserBuff::getUserId, userId)
                .eq(FarmUserBuff::getBuffType, buffType.intValue())
                .ge(FarmUserBuff::getLevel, minLevel)) > 0;
    }

    private Long parseLong(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
