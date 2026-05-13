package com.cong.fishisland.model.dto.redeem;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 创建兑换码请求（管理员）
 *
 * @author cong
 */
@Data
public class RedeemCodeAddRequest implements Serializable {

    /**
     * 自定义兑换码，不填则自动生成
     */
    private String code;

    /**
     * 类型：1-通用码 2-专属码
     */
    private Integer type;

    /**
     * 专属码绑定的目标用户ID（type=2 且需要指定用户时填写）
     */
    private Long targetUserId;

    /**
     * 奖励类型：1-积分 2-会员天数 3-道具 4-称号 5-头像框
     */
    private Integer rewardType;

    /**
     * 奖励值：积分数量/会员天数/道具ID/称号ID/头像框ID
     */
    private Long rewardValue;

    /**
     * 奖励数量（道具类有效，默认1）
     */
    private Integer rewardCount;

    /**
     * 兑换码描述/备注
     */
    private String description;

    /**
     * 过期时间，不填则永不过期
     */
    private Date expireTime;

    /**
     * 最大使用次数：-1不限（通用码），专属码固定为1
     */
    private Integer maxUseCount;

    /**
     * 批量生成数量（仅通用码/专属码批量生成时有效，最大100）
     */
    private Integer batchCount;

    private static final long serialVersionUID = 1L;
}
