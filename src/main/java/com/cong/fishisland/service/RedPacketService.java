package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.redpacket.CreateRedPacketRequest;
import com.cong.fishisland.model.entity.redpacket.RedPacket;
import com.cong.fishisland.model.vo.redpacket.RedPacketRecordVO;

import java.util.List;

/**
 * 红包服务接口
 * @author cong
 */
public interface RedPacketService {
    
    /**
     * 创建红包
     * 注意：VIP用户每日第一个红包不需要花积分
     *
     * @param request 创建红包请求
     * @return 红包ID
     */
    String createRedPacket(CreateRedPacketRequest request);
    
    /**
     * 抢红包
     *
     * @param redPacketId 红包ID
     * @param userId 用户ID
     * @return 抢到的金额，如果抢不到返回null
     */
    Integer grabRedPacket(String redPacketId, Long userId);

    
    /**
     * 获取红包抢购记录
     *
     * @param redPacketId 红包ID
     * @return 抢购记录列表
     */
    List<RedPacketRecordVO> getRedPacketRecords(String redPacketId);

    RedPacket getRedPacketDetail(String redPacketId);
}