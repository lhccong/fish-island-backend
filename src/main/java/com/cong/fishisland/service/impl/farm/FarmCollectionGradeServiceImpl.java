package com.cong.fishisland.service.impl.farm;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.farm.FarmCollectionGradeMapper;
import com.cong.fishisland.mapper.farm.FarmCropMapper;
import com.cong.fishisland.model.dto.farm.CollectionGradeEntryVO;
import com.cong.fishisland.model.dto.farm.CollectionGradeGroupVO;
import com.cong.fishisland.model.dto.farm.CollectionRollResult;
import com.cong.fishisland.model.entity.farm.FarmCollectionGrade;
import com.cong.fishisland.model.entity.farm.FarmCrop;
import com.cong.fishisland.model.enums.farm.FarmConstants;
import com.cong.fishisland.service.FarmAchievementService;
import com.cong.fishisland.service.FarmCollectionGradeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 作物图鉴服务实现：收获时独立 roll 品级与重量，同作物同品级仅保留最大重量记录。
 */
@Slf4j
@Service
public class FarmCollectionGradeServiceImpl implements FarmCollectionGradeService {

    @Resource
    private FarmCollectionGradeMapper collectionGradeMapper;

    @Resource
    private FarmCropMapper cropMapper;

    @Resource
    private FarmAchievementService achievementService;

    @Override
    public CollectionRollResult rollOnHarvest(Long userId, FarmCrop crop, int luckyBoostPercent) {
        if (userId == null || crop == null || crop.getId() == null) {
            return null;
        }
        try {
            int grade = sampleGrade(crop.getGradeWeightJson(), luckyBoostPercent);
            int weight = sampleWeight(crop, grade, luckyBoostPercent);

            FarmCollectionGrade existing = collectionGradeMapper.selectOne(
                    new LambdaQueryWrapper<FarmCollectionGrade>()
                            .eq(FarmCollectionGrade::getUserId, userId)
                            .eq(FarmCollectionGrade::getCropId, crop.getId())
                            .eq(FarmCollectionGrade::getGrade, grade));
            boolean newEntry = existing == null;
            // 图鉴记录被更新 = 新建，或本次重量刷新了该品级的最大重量
            boolean updated = newEntry
                    || (existing.getMaxWeight() == null || weight > existing.getMaxWeight());

            collectionGradeMapper.upsertOnHeavier(userId, crop.getId(), grade, weight);

            if (newEntry) {
                log.info("图鉴解锁 userId={}, cropId={}, grade={}, weight={}", userId, crop.getId(), grade, weight);
                achievementService.onProgress(userId,
                        com.cong.fishisland.model.enums.farm.FarmAchievementTypeEnum.COLLECTION_UNLOCK,
                        null, grade);
            } else if (updated) {
                log.info("图鉴重量刷新 userId={}, cropId={}, grade={}, weight={}（原 {}）",
                        userId, crop.getId(), grade, weight, existing.getMaxWeight());
            }
            return new CollectionRollResult(newEntry, updated, grade, weight, crop.getId());
        } catch (Exception e) {
            // 图鉴 roll 失败不影响收获主流程
            log.error("图鉴 roll 异常 userId={}, cropId={}", userId, crop.getId(), e);
            return null;
        }
    }

    @Override
    public long countEntriesWithGradeAtLeast(Long userId, int minGrade) {
        if (userId == null) {
            return 0;
        }
        return collectionGradeMapper.selectCount(new LambdaQueryWrapper<FarmCollectionGrade>()
                .eq(FarmCollectionGrade::getUserId, userId)
                .ge(FarmCollectionGrade::getGrade, minGrade));
    }

    @Override
    public List<CollectionGradeGroupVO> getMyCollection(Long userId) {
        List<FarmCollectionGrade> records = collectionGradeMapper.selectList(
                new LambdaQueryWrapper<FarmCollectionGrade>()
                        .eq(FarmCollectionGrade::getUserId, userId)
                        .orderByAsc(FarmCollectionGrade::getCropId)
                        .orderByAsc(FarmCollectionGrade::getGrade));
        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> cropIds = records.stream().map(FarmCollectionGrade::getCropId)
                .distinct().collect(Collectors.toList());
        Map<Long, FarmCrop> cropMap = cropMapper.selectBatchIds(cropIds).stream()
                .collect(Collectors.toMap(FarmCrop::getId, Function.identity()));

        Map<Long, List<FarmCollectionGrade>> byCrop = records.stream()
                .collect(Collectors.groupingBy(FarmCollectionGrade::getCropId));

        List<CollectionGradeGroupVO> groups = new ArrayList<>(byCrop.size());
        for (Map.Entry<Long, List<FarmCollectionGrade>> entry : byCrop.entrySet()) {
            FarmCrop crop = cropMap.get(entry.getKey());
            List<FarmCollectionGrade> cropRecords = entry.getValue();
            List<CollectionGradeEntryVO> entries = new ArrayList<>(cropRecords.size());
            for (FarmCollectionGrade record : cropRecords) {
                entries.add(new CollectionGradeEntryVO(
                        record.getGrade(),
                        gradeIcon(crop, record.getGrade()),
                        record.getMaxWeight(),
                        record.getFirstObtainedTime()));
            }
            int unlocked = entries.size();
            groups.add(new CollectionGradeGroupVO(
                    entry.getKey(),
                    crop != null ? crop.getName() : "未知作物",
                    crop != null ? crop.getIcon() : "",
                    entries,
                    unlocked,
                    Math.round(unlocked * 10000.0 / FarmConstants.COLLECTION_GRADE_MAX) / 100.0));
        }
        return groups;
    }

    /**
     * 按权重抽样品级；神奇加成仅放大高品级（≥ LUCKY_BOOST_MIN_GRADE）的权重，归一化后抽样。
     */
    private int sampleGrade(String gradeWeightJson, int luckyBoostPercent) {
        int[] weights = parseGradeWeights(gradeWeightJson);
        double boost = 1 + Math.max(0, luckyBoostPercent) / 100.0;
        double total = 0;
        double[] effective = new double[weights.length];
        for (int i = 0; i < weights.length; i++) {
            int grade = i + 1;
            effective[i] = weights[i] * (grade >= FarmConstants.LUCKY_BOOST_MIN_GRADE ? boost : 1.0);
            total += effective[i];
        }
        double roll = ThreadLocalRandom.current().nextDouble() * total;
        double cumulative = 0;
        for (int i = 0; i < effective.length; i++) {
            cumulative += effective[i];
            if (roll < cumulative) {
                return i + 1;
            }
        }
        return 1;
    }

    /**
     * 重量 = 初始重量 × 品级乘数 × 神奇加成，品级内 ±浮动。
     */
    private int sampleWeight(FarmCrop crop, int grade, int luckyBoostPercent) {
        int baseWeight = crop.getBaseWeight() != null ? crop.getBaseWeight() : 100;
        double bonus = 1 + Math.max(0, luckyBoostPercent) / 100.0;
        double base = baseWeight * FarmConstants.weightMultiplier(grade) * bonus;
        double fluct = FarmConstants.GRADE_WEIGHT_FLUCTUATION;
        double factor = 1 - fluct + ThreadLocalRandom.current().nextDouble() * 2 * fluct;
        return Math.max(1, (int) Math.round(base * factor));
    }

    private int[] parseGradeWeights(String gradeWeightJson) {
        if (StringUtils.isBlank(gradeWeightJson)) {
            return FarmConstants.DEFAULT_GRADE_WEIGHTS;
        }
        try {
            JSONArray array = JSONUtil.parseArray(gradeWeightJson);
            int[] weights = new int[FarmConstants.COLLECTION_GRADE_MAX];
            int filled = 0;
            for (Object item : array) {
                int weight = Integer.parseInt(String.valueOf(item));
                if (weight < 0) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "品级权重不能为负");
                }
                if (filled < weights.length) {
                    weights[filled++] = weight;
                }
            }
            long sum = 0;
            for (int weight : weights) {
                sum += weight;
            }
            if (sum <= 0) {
                return FarmConstants.DEFAULT_GRADE_WEIGHTS;
            }
            return weights;
        } catch (Exception e) {
            log.warn("品级权重 JSON 解析失败，使用默认曲线：{}", gradeWeightJson);
            return FarmConstants.DEFAULT_GRADE_WEIGHTS;
        }
    }

    private String gradeIcon(FarmCrop crop, int grade) {
        if (crop != null && StringUtils.isNotBlank(crop.getCollectionIconsJson())) {
            try {
                JSONArray array = JSONUtil.parseArray(crop.getCollectionIconsJson());
                if (array.size() >= grade) {
                    return array.getStr(grade - 1);
                }
            } catch (Exception ignored) {
                // 图标 JSON 异常时回退作物图标
            }
        }
        return crop != null ? crop.getIcon() : "";
    }
}
