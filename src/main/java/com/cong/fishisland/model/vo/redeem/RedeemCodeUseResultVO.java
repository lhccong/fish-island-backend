package com.cong.fishisland.model.vo.redeem;

import lombok.Data;

import java.io.Serializable;

/**
 * 兑换码使用结果 VO
 *
 * @author cong
 */
@Data
public class RedeemCodeUseResultVO implements Serializable {

    /**
     * 兑换码
     */
    private String code;

    /**
     * 奖励类型：1-积分 2-会员天数 3-道具 4-称号 5-头像框
     */
    private Integer rewardType;

    /**
     * 奖励类型名称
     */
    private String rewardTypeName;

    /**
     * 奖励值
     */
    private Long rewardValue;

    /**
     * 奖励数量
     */
    private Integer rewardCount;

    /**
     * 兑换描述（如：恭喜获得 500 积分）
     */
    private String message;

    private static final long serialVersionUID = 1L;
}
