package com.cong.fishisland.model.dto.user;

import com.cong.fishisland.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户会员查询请求
 *
 * @author cong
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserVipQueryRequest extends PageRequest implements Serializable {

    /**
     * 会员ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会员兑换卡号（永久会员无卡号）
     */
    private String cardNo;

    /**
     * 会员类型（1-月卡会员 2-永久会员）
     */
    private Integer type;

    /**
     * 创建开始时间
     */
    private String createTimeStart;

    /**
     * 创建结束时间
     */
    private String createTimeEnd;

    /**
     * 更新开始时间
     */
    private String updateTimeStart;

    /**
     * 更新结束时间
     */
    private String updateTimeEnd;

    private static final long serialVersionUID = 1L;
} 