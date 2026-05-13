package com.cong.fishisland.model.enums.redeem;

import lombok.Getter;

/**
 * 兑换码奖励类型枚举
 *
 * @author cong
 */
@Getter
public enum RedeemRewardTypeEnum {

    POINTS(1, "积分"),
    VIP_DAYS(2, "会员天数"),
    PROPS(3, "道具"),
    TITLE(4, "称号"),
    AVATAR_FRAME(5, "头像框");

    private final int value;
    private final String text;

    RedeemRewardTypeEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public static RedeemRewardTypeEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (RedeemRewardTypeEnum e : values()) {
            if (e.value == value) {
                return e;
            }
        }
        return null;
    }
}
