package com.cong.fishisland.service.turntable.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.mapper.turntable.TurntableMapper;
import com.cong.fishisland.model.dto.turntable.DrawRequest;
import com.cong.fishisland.model.dto.turntable.TurntableDrawRecordQueryRequest;
import com.cong.fishisland.model.dto.turntable.TurntableQueryRequest;
import com.cong.fishisland.model.entity.pet.ItemInstances;
import com.cong.fishisland.model.entity.pet.ItemTemplates;
import com.cong.fishisland.model.entity.turntable.Turntable;
import com.cong.fishisland.model.entity.turntable.TurntableDrawRecord;
import com.cong.fishisland.model.entity.turntable.TurntablePrize;
import com.cong.fishisland.model.entity.turntable.TurntableUserProgress;
import com.cong.fishisland.model.entity.user.UserTitle;
import com.cong.fishisland.model.vo.turntable.*;
import com.cong.fishisland.service.ItemInstancesService;
import com.cong.fishisland.service.ItemTemplatesService;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.UserTitleService;
import com.cong.fishisland.service.turntable.*;
import com.cong.fishisland.service.turntable.strategy.DrawStrategy;
import com.cong.fishisland.service.turntable.strategy.impl.GuaranteeDrawStrategy;
import com.cong.fishisland.service.turntable.strategy.impl.WeightRandomDrawStrategy;
import com.cong.fishisland.utils.SqlUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 转盘服务实现
 * @author cong
 */
@Service
public class TurntableServiceImpl extends ServiceImpl<TurntableMapper, Turntable> implements TurntableService {

    @Resource
    private TurntablePrizeService turntablePrizeService;

    @Resource
    private TurntableDrawRecordService turntableDrawRecordService;

    @Resource
    private TurntableUserProgressService turntableUserProgressService;

    @Resource
    private UserPointsService userPointsService;

    @Resource
    private ItemInstancesService itemInstancesService;

    @Resource
    private ItemTemplatesService itemTemplatesService;

    @Resource
    private UserTitleService userTitleService;

    @Resource
    private WeightRandomDrawStrategy weightRandomDrawStrategy;

    @Resource
    private GuaranteeDrawStrategy guaranteeDrawStrategy;

    /**
     * 小保底触发次数
     */
    private static final int SMALL_GUARANTEE_COUNT = 10;

    /**
     * 大保底触发次数
     */
    private static final int BIG_GUARANTEE_COUNT = 300;

    @Override
    public List<TurntableVO> listActiveTurntables(TurntableQueryRequest turntableQueryRequest) {
        QueryWrapper<Turntable> queryWrapper = getQueryWrapper(turntableQueryRequest);
        queryWrapper.eq("status", 1);
        queryWrapper.eq("isDelete", 0);
        List<Turntable> turntables = this.list(queryWrapper);
        
        return turntables.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public TurntableVO getTurntableDetail(Long turntableId) {
        ThrowUtils.throwIf(turntableId == null || turntableId <= 0, ErrorCode.PARAMS_ERROR, "转盘ID非法");
        
        Turntable turntable = this.getById(turntableId);
        ThrowUtils.throwIf(turntable == null || turntable.getIsDelete() == 1, ErrorCode.NOT_FOUND_ERROR, "转盘不存在");
        
        TurntableVO vo = convertToVO(turntable);
        
        // 获取奖品列表
        List<TurntablePrize> prizes = turntablePrizeService.listByTurntableId(turntableId);
        List<TurntablePrizeVO> prizeVOList = prizes.stream().map(this::convertPrizeToVO).collect(Collectors.toList());
        vo.setPrizeList(prizeVOList);
        
        // 获取用户进度
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());
        TurntableUserProgress progress = turntableUserProgressService.getOrCreateProgress(userId, turntableId, turntable.getGuaranteeCount());
        vo.setUserProgress(convertProgressToVO(progress));
        
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DrawResultVO draw(DrawRequest drawRequest) {
        // 参数校验
        ThrowUtils.throwIf(drawRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        Long turntableId = drawRequest.getTurntableId();
        Integer drawCount = drawRequest.getDrawCount();
        ThrowUtils.throwIf(turntableId == null || turntableId <= 0, ErrorCode.PARAMS_ERROR, "转盘ID非法");
        ThrowUtils.throwIf(drawCount == null || drawCount <= 0, ErrorCode.PARAMS_ERROR, "抽奖次数必须大于0");
        ThrowUtils.throwIf(drawCount > 10, ErrorCode.PARAMS_ERROR, "单次抽奖次数不能超过10次");

        // 获取转盘信息
        Turntable turntable = this.getById(turntableId);
        ThrowUtils.throwIf(turntable == null || turntable.getIsDelete() == 1, ErrorCode.NOT_FOUND_ERROR, "转盘不存在");
        ThrowUtils.throwIf(turntable.getStatus() != 1, ErrorCode.OPERATION_ERROR, "转盘未启用");

        // 获取用户ID
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());

        // 计算消耗积分
        int totalCostPoints = turntable.getCostPoints() * drawCount;

        // 扣除积分
        userPointsService.deductPoints(userId, totalCostPoints);

        // 获取奖品列表
        List<TurntablePrize> prizes = turntablePrizeService.listAvailableByTurntableId(turntableId);
        ThrowUtils.throwIf(prizes == null || prizes.isEmpty(), ErrorCode.OPERATION_ERROR, "转盘没有可用奖品");

        // 获取用户进度
        TurntableUserProgress progress = turntableUserProgressService.getOrCreateProgress(userId, turntableId, turntable.getGuaranteeCount());

        // 执行抽奖
        List<DrawPrizeVO> drawResults = new ArrayList<>();
        boolean isGuaranteeTriggered = false;
        int guaranteeType = 0;

        for (int i = 0; i < drawCount; i++) {
            // 检查是否触发保底
            GuaranteeCheckResult checkResult = checkGuarantee(progress, drawCount - i);
            
            TurntablePrize selectedPrize;
            if (checkResult.isTriggered) {
                // 触发保底，使用保底策略
                guaranteeDrawStrategy.setMinQuality(checkResult.minQuality);
                selectedPrize = guaranteeDrawStrategy.draw(prizes);
                isGuaranteeTriggered = true;
                guaranteeType = checkResult.guaranteeType;
            } else {
                // 未触发保底，使用权重随机策略
                selectedPrize = weightRandomDrawStrategy.draw(prizes);
            }

            if (selectedPrize == null) {
                // 如果没有抽中，随机选一个
                selectedPrize = prizes.get(new Random().nextInt(prizes.size()));
            }

            // 构建抽奖结果
            DrawPrizeVO prizeVO = buildDrawPrizeVO(selectedPrize, checkResult.isTriggered);
            drawResults.add(prizeVO);

            // 更新进度（用于后续抽奖判断）
            updateProgressForDraw(progress, selectedPrize, checkResult.isTriggered, checkResult.guaranteeType);
        }

        // 保存抽奖记录
        saveDrawRecords(userId, turntableId, drawResults, totalCostPoints);

        // 更新用户进度
        turntableUserProgressService.updateProgress(userId, turntableId, isGuaranteeTriggered, guaranteeType, drawCount);

        // 发放奖励
        deliverPrizes(userId, drawResults);

        // 构建返回结果
        DrawResultVO resultVO = new DrawResultVO();
        resultVO.setPrizeList(drawResults);
        resultVO.setIsGuarantee(isGuaranteeTriggered);
        resultVO.setGuaranteeType(guaranteeType > 0 ? guaranteeType : null);
        resultVO.setCostPoints(totalCostPoints);

        return resultVO;
    }

    @Override
    public List<DrawRecordVO> listDrawRecords(TurntableDrawRecordQueryRequest queryRequest) {
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());
        
        QueryWrapper<TurntableDrawRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        if (queryRequest != null && queryRequest.getTurntableId() != null) {
            queryWrapper.eq("turntableId", queryRequest.getTurntableId());
        }
        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderByDesc("createTime");
        
        List<TurntableDrawRecord> records = turntableDrawRecordService.list(queryWrapper);
        
        return records.stream().map(this::convertRecordToVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<Turntable> getQueryWrapper(TurntableQueryRequest turntableQueryRequest) {
        QueryWrapper<Turntable> queryWrapper = new QueryWrapper<>();
        if (turntableQueryRequest == null) {
            return queryWrapper;
        }
        
        if (turntableQueryRequest.getType() != null) {
            queryWrapper.eq("type", turntableQueryRequest.getType());
        }
        
        String sortField = turntableQueryRequest.getSortField();
        String sortOrder = turntableQueryRequest.getSortOrder();
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        
        if (StringUtils.isNotBlank(sortField) && SqlUtils.validSortField(sortField)) {
            queryWrapper.orderBy(true, asc, sortField);
        } else {
            queryWrapper.orderByDesc("createTime");
        }
        
        return queryWrapper;
    }

    /**
     * 检查是否触发保底
     */
    private GuaranteeCheckResult checkGuarantee(TurntableUserProgress progress, int remainingDraws) {
        GuaranteeCheckResult result = new GuaranteeCheckResult();
        
        int totalDrawCount = progress.getTotalDrawCount() + (progress.getTotalDrawCount() > 0 ? 1 : 0);
        int smallFailCount = progress.getSmallFailCount();
        
        // 检查大保底（累计抽奖次数达到阈值）
        if (totalDrawCount + remainingDraws >= BIG_GUARANTEE_COUNT) {
            result.isTriggered = true;
            result.guaranteeType = 2;
            // 史诗及以上
            result.minQuality = 3;
            return result;
        }
        
        // 检查小保底（连续未抽中高品质次数达到阈值）
        if (smallFailCount + remainingDraws >= SMALL_GUARANTEE_COUNT) {
            result.isTriggered = true;
            result.guaranteeType = 1;
            // 稀有及以上
            result.minQuality = 2;
            return result;
        }
        
        result.isTriggered = false;
        return result;
    }

    /**
     * 更新进度（用于单次抽奖后的进度更新）
     */
    private void updateProgressForDraw(TurntableUserProgress progress, TurntablePrize prize, boolean isGuarantee, int guaranteeType) {
        // 如果抽中高品质（稀有及以上），重置小保底计数
        if (prize.getQuality() != null && prize.getQuality() >= 2) {
            progress.setSmallFailCount(0);
        } else {
            progress.setSmallFailCount(progress.getSmallFailCount() + 1);
        }
        
        // 累计抽奖次数
        progress.setTotalDrawCount(progress.getTotalDrawCount() + 1);
        
        // 如果触发保底，重置相应计数
        if (isGuarantee) {
            if (guaranteeType == 1) {
                progress.setSmallFailCount(0);
            } else if (guaranteeType == 2) {
                progress.setTotalDrawCount(Math.max(0, progress.getTotalDrawCount() - BIG_GUARANTEE_COUNT));
                progress.setSmallFailCount(0);
            }
        }
    }

    /**
     * 构建抽奖奖品VO
     */
    private DrawPrizeVO buildDrawPrizeVO(TurntablePrize prize, boolean isGuarantee) {
        DrawPrizeVO vo = new DrawPrizeVO();
        vo.setPrizeId(prize.getPrizeId());
        vo.setTurntablePrizeId(prize.getId());
        vo.setQuality(prize.getQuality());
        vo.setQualityName(getQualityName(prize.getQuality()));
        vo.setPrizeType(prize.getPrizeType());
        vo.setConvertedToPoints(false);
        vo.setConvertedPoints(0);

        // 获取奖品名称和图片
        if (prize.getPrizeType() == 1) {
            // 装备类型
            ItemTemplates template = itemTemplatesService.getById(prize.getPrizeId());
            if (template != null) {
                vo.setName(template.getName());
                vo.setIcon(template.getIcon());
                vo.setConvertedPoints(template.getRemovePoint());
            }
        } else if (prize.getPrizeType() == 2) {
            // 称号类型
            UserTitle title = userTitleService.getById(prize.getPrizeId());
            if (title != null) {
                vo.setName(title.getName());
                vo.setIcon(title.getTitleImg());
            }
        }

        return vo;
    }

    /**
     * 保存抽奖记录
     */
    private void saveDrawRecords(Long userId, Long turntableId, List<DrawPrizeVO> prizes, int totalCostPoints) {
        List<TurntableDrawRecord> records = prizes.stream().map(prize -> {
            TurntableDrawRecord record = new TurntableDrawRecord();
            record.setUserId(userId);
            record.setTurntableId(turntableId);
            record.setTurntablePrizeId(prize.getTurntablePrizeId());
            record.setPrizeId(prize.getPrizeId());
            record.setName(prize.getName());
            record.setPrizeType(prize.getPrizeType());
            record.setQuality(prize.getQuality());
            record.setCostPoints(totalCostPoints / prizes.size());
            record.setIsGuarantee(prize.getConvertedToPoints() ? 0 : (prize.getQuality() >= 2 ? 1 : 0));
            return record;
        }).collect(Collectors.toList());

        turntableDrawRecordService.saveBatchRecords(records);
    }

    /**
     * 发放奖励
     */
    private void deliverPrizes(Long userId, List<DrawPrizeVO> prizes) {
        for (DrawPrizeVO prize : prizes) {
            try {
                if (prize.getPrizeType() == 1) {
                    // 装备类型 - 添加到用户背包
                    ItemInstances instance = new ItemInstances();
                    instance.setTemplateId(prize.getPrizeId());
                    instance.setOwnerUserId(userId);
                    instance.setQuantity(1);
                    instance.setBound(1);
                    itemInstancesService.save(instance);
                } else if (prize.getPrizeType() == 2) {
                    // 称号类型 - 添加到用户称号列表
                    userTitleService.addTitleToUser(userId, prize.getPrizeId());
                }
            } catch (Exception e) {
                // 如果发放失败，转换为积分
                if (prize.getConvertedPoints() != null && prize.getConvertedPoints() > 0) {
                    userPointsService.updateUsedPoints(userId, -prize.getConvertedPoints());
                    prize.setConvertedToPoints(true);
                }
            }
        }
    }

    /**
     * 转换为VO
     */
    private TurntableVO convertToVO(Turntable turntable) {
        TurntableVO vo = new TurntableVO();
        BeanUtils.copyProperties(turntable, vo);
        return vo;
    }

    /**
     * 转换奖品为VO
     */
    private TurntablePrizeVO convertPrizeToVO(TurntablePrize prize) {
        TurntablePrizeVO vo = new TurntablePrizeVO();
        BeanUtils.copyProperties(prize, vo);
        vo.setQualityName(getQualityName(prize.getQuality()));

        // 获取奖品名称和图片
        if (prize.getPrizeType() == 1) {
            ItemTemplates template = itemTemplatesService.getById(prize.getPrizeId());
            if (template != null) {
                vo.setName(template.getName());
                vo.setIcon(template.getIcon());
            }
        } else if (prize.getPrizeType() == 2) {
            UserTitle title = userTitleService.getById(prize.getPrizeId());
            if (title != null) {
                vo.setName(title.getName());
                vo.setIcon(title.getTitleImg());
            }
        }

        return vo;
    }

    /**
     * 转换进度为VO
     */
    private UserProgressVO convertProgressToVO(TurntableUserProgress progress) {
        UserProgressVO vo = new UserProgressVO();
        BeanUtils.copyProperties(progress, vo);
        return vo;
    }

    /**
     * 转换记录为VO
     */
    private DrawRecordVO convertRecordToVO(TurntableDrawRecord record) {
        DrawRecordVO vo = new DrawRecordVO();
        BeanUtils.copyProperties(record, vo);
        vo.setIsGuarantee(record.getIsGuarantee() != null && record.getIsGuarantee() == 1);
        vo.setQualityName(getQualityName(record.getQuality()));

        // 获取奖品图片
        if (record.getPrizeType() == 1) {
            ItemTemplates template = itemTemplatesService.getById(record.getPrizeId());
            if (template != null) {
                vo.setIcon(template.getIcon());
            }
        } else if (record.getPrizeType() == 2) {
            UserTitle title = userTitleService.getById(record.getPrizeId());
            if (title != null) {
                vo.setIcon(title.getTitleImg());
            }
        }

        return vo;
    }

    /**
     * 获取品质名称
     */
    private String getQualityName(Integer quality) {
        if (quality == null) {
            return "普通";
        }
        switch (quality) {
            case 1:
                return "普通(N)";
            case 2:
                return "稀有(R)";
            case 3:
                return "史诗(SR)";
            case 4:
                return "传说(SSR)";
            default:
                return "普通";
        }
    }

    /**
     * 保底检查结果
     */
    private static class GuaranteeCheckResult {
        boolean isTriggered;
        int guaranteeType;
        int minQuality;
    }
}
