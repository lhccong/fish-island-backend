package com.cong.fishisland.model.dto.redeem;

import com.cong.fishisland.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 兑换码查询请求（管理员）
 *
 * @author cong
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RedeemCodeQueryRequest extends PageRequest implements Serializable {

    /**
     * 兑换码
     */
    private String code;

    /**
     * 类型：1-通用码 2-专属码
     */
    private Integer type;

    /**
     * 状态：0-已禁用 1-正常 2-已用完
     */
    private Integer status;

    /**
     * 奖励类型
     */
    private Integer rewardType;

    private static final long serialVersionUID = 1L;
}
