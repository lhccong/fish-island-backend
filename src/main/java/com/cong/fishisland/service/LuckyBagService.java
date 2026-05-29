package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.luckybag.CreateLuckyBagRequest;
import com.cong.fishisland.model.entity.luckybag.LuckyBag;
import com.cong.fishisland.model.vo.luckybag.LuckyBagRecordVO;

import java.util.List;

/**
 * 福袋服务
 */
public interface LuckyBagService {

    /**
     * 创建福袋并广播到聊天室
     */
    LuckyBag createLuckyBag(CreateLuckyBagRequest request);

    /**
     * 参与福袋
     */
    void joinLuckyBag(String luckyBagId, Long userId);

    /**
     * 福袋详情
     */
    LuckyBag getLuckyBagDetail(String luckyBagId);

    /**
     * 中奖记录（开奖后）
     */
    List<LuckyBagRecordVO> getLuckyBagWinRecords(String luckyBagId);

    /**
     * 获取当前进行中的福袋列表
     */
    List<LuckyBag> getActiveLuckyBags();

    /**
     * 处理已到期的福袋（定时任务调用）
     */
    void processExpiredLuckyBags();
}
