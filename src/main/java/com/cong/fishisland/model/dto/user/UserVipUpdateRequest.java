package com.cong.fishisland.model.dto.user;

import com.cong.fishisland.constant.VipTypeConstant;
import com.cong.fishisland.model.enums.VipTypeEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户会员更新请求
 *
 * @author cong
 */
@Data
public class UserVipUpdateRequest implements Serializable {

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
     * 会员类型
     * {@link VipTypeEnum}
     * {@link VipTypeConstant#MONTHLY} - 月卡会员
     * {@link VipTypeConstant#PERMANENT} - 永久会员
     */
    private Integer type;

    /**
     * 会员到期时间，永久会员为null
     */
    private Date validDays;

    private static final long serialVersionUID = 1L;
} 