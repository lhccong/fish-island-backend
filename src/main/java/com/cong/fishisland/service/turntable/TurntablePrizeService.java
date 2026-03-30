package com.cong.fishisland.service.turntable;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.turntable.TurntablePrize;

import java.util.List;

/**
 * 转盘奖励服务接口
 * @author cong
 */
public interface TurntablePrizeService extends IService<TurntablePrize> {

    /**
     * 根据转盘ID获取奖品列表
     * @param turntableId 转盘ID
     * @return 奖品列表
     */
    List<TurntablePrize> listByTurntableId(Long turntableId);

    /**
     * 根据转盘ID获取奖品列表（只获取有库存的）
     * @param turntableId 转盘ID
     * @return 奖品列表
     */
    List<TurntablePrize> listAvailableByTurntableId(Long turntableId);
}
