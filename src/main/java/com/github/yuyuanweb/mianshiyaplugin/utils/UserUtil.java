package com.github.yuyuanweb.mianshiyaplugin.utils;

import com.github.yuyuanweb.mianshiyaplugin.model.enums.UserRoleEnum;
import com.github.yuyuanweb.mianshiyaplugin.model.response.User;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author pine
 */
public class UserUtil {

    /**
     * VIP 角色枚举列表
     */
    public static final List<UserRoleEnum> VIP_ROLE_ENUM_LIST = Arrays.asList(
            UserRoleEnum.VIP,
            UserRoleEnum.SVIP,
            UserRoleEnum.FVIP
    );

    public static final List<UserRoleEnum> VIP_ROLE_LIMIT_DATE_ENUM_LIST = Arrays.asList(
            UserRoleEnum.VIP,
            UserRoleEnum.SVIP
    );


    public static boolean hasVipAuth(User user) {
        if (user == null) {
            return false;
        }
        String userRole = user.getUserRole();
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(userRole);
        Date vipExpireTime = user.getVipExpireTime();
        // 是管理员或运营，有权限
        if (UserRoleEnum.ADMIN.equals(userRoleEnum) || UserRoleEnum.OPERATOR.equals(userRoleEnum)) {
            return true;
        }
        // 不是 VIP 或超级 VIP 或永久 VIP
        if (!VIP_ROLE_ENUM_LIST.contains(userRoleEnum)) {
            return false;
        }
        // VIP 已过期
        if (VIP_ROLE_LIMIT_DATE_ENUM_LIST.contains(userRoleEnum)) {
            return vipExpireTime != null && !vipExpireTime.before(new Date());
        }
        return true;
    }
}
