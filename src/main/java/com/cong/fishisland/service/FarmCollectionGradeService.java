package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.farm.CollectionGradeGroupVO;
import com.cong.fishisland.model.dto.farm.CollectionRollResult;
import com.cong.fishisland.model.entity.farm.FarmCrop;

import java.util.List;

/**
 * 作物图鉴（品级×最大重量）服务
 */
public interface FarmCollectionGradeService {

    /**
     * 收获时 roll 图鉴：独立 roll 品级与重量，同作物同品级仅保留最大重量记录。
     *
     * @param userId             用户ID
     * @param crop               作物
     * @param luckyBoostPercent  神奇生长光环加成（百分比，0 表示无加成）
     * @return roll 结果（newEntry=true 表示解锁了新图鉴）；异常时返回 null，不影响收获主流程
     */
    CollectionRollResult rollOnHarvest(Long userId, FarmCrop crop, int luckyBoostPercent);

    /**
     * 统计用户已解锁的品级 ≥ minGrade 的图鉴条目数（buff 升级门槛校验）。
     */
    long countEntriesWithGradeAtLeast(Long userId, int minGrade);

    /**
     * 我的图鉴（按作物分组，仅包含已有记录的作物）。
     */
    List<CollectionGradeGroupVO> getMyCollection(Long userId);
}
