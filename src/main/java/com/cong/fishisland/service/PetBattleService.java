package com.cong.fishisland.service;

import com.cong.fishisland.model.vo.game.PetBattleInfoVO;
import com.cong.fishisland.model.vo.game.PetBattleResultVO;

import java.util.List;

/**
 * 宠物对战服务接口
 *
 * @author cong
 */
public interface PetBattleService {

    /**
     * 获取宠物对战信息（双方宠物详情）
     *
     * @param opponentUserId 对手用户ID
     * @return 对战信息
     */
    PetBattleInfoVO getPetBattleInfo(Long opponentUserId);

    /**
     * 宠物对战
     *
     * @param opponentUserId 对手用户ID
     * @return 对战回合结果列表
     */
    List<PetBattleResultVO> battle(Long opponentUserId);
}
