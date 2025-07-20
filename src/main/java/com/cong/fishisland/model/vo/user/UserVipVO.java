package com.cong.fishisland.model.vo.user;

import com.cong.fishisland.constant.VipTypeConstant;
import com.cong.fishisland.model.entity.user.UserVip;
import com.cong.fishisland.model.enums.VipTypeEnum;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户会员视图对象
 *
 * @author cong
 */
@Data
public class UserVipVO implements Serializable {

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

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 用户信息
     */
    private UserVO user;

    /**
     * 是否已过期
     */
    private Boolean isExpired;

    /**
     * 对象转包装类
     *
     * @param userVip 用户会员
     * @return {@link UserVipVO}
     */
    public static UserVipVO objToVo(UserVip userVip) {
        if (userVip == null) {
            return null;
        }
        UserVipVO userVipVO = new UserVipVO();
        BeanUtils.copyProperties(userVip, userVipVO);
        
        // 判断会员是否已过期
        if (VipTypeConstant.PERMANENT.equals(userVip.getType())) {
            // 永久会员不会过期
            userVipVO.setIsExpired(false);
        } else {
            // 月卡会员，判断是否过期
            Date now = new Date();
            userVipVO.setIsExpired(userVip.getValidDays() != null && now.after(userVip.getValidDays()));
        }
        
        return userVipVO;
    }

    private static final long serialVersionUID = 1L;
} 