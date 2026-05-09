package com.cong.fishisland.model.vo.redeem;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 兑换码 VO（管理员视图）
 *
 * @author cong
 */
@Data
public class RedeemCodeVO implements Serializable {

    /**
     * 兑换码ID
     */
    private Long id;

    /**
     * 兑换码
     */
    private String code;

    /**
     * 类型：1-通用码 2-专属码
     */
    private Integer type;

    /**
     * 类型名称
     */
    private String typeName;

    /**
     * 专属码绑定的目标用户ID
     */
    private Long targetUserId;

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
     * 描述
     */
    private String description;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 状态：0-已禁用 1-正常 2-已用完
     */
    private Integer status;

    /**
     * 已使用次数
     */
    private Integer usedCount;

    /**
     * 最大使用次数
     */
    private Integer maxUseCount;

    /**
     * 创建时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
