package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.farm.FarmBuffVO;
import com.cong.fishisland.model.entity.farm.FarmUserBuff;
import com.cong.fishisland.model.enums.farm.FarmBuffTypeEnum;

import java.util.List;

/**
 * 农场 buff 道具服务
 */
public interface FarmUserBuffService {

    /**
     * 商城/背包/农田左侧：4 类道具的持有状态与购买/升级信息。
     */
    List<FarmBuffVO> getMyBuffs(Long userId);

    /**
     * 指定用户指定 buff 的当前效果百分比（未拥有返回 0）。
     */
    int getBuffPercent(Long userId, FarmBuffTypeEnum type);

    /**
     * 购买 buff（校验解锁条件与积分，自动生效）。
     */
    FarmUserBuff purchase(Integer buffType);

    /**
     * 升级 buff（校验图鉴门槛与积分；加速道具升级后对生长中作物实时生效）。
     */
    FarmUserBuff upgrade(Integer buffType);
}
