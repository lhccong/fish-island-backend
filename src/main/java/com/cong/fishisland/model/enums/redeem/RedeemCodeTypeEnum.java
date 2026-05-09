package com.cong.fishisland.model.enums.redeem;

import lombok.Getter;

/**
 * 兑换码类型枚举
 *
 * @author cong
 */
@Getter
public enum RedeemCodeTypeEnum {

    GENERAL(1, "通用码"),
    EXCLUSIVE(2, "专属码");

    private final int value;
    private final String text;

    RedeemCodeTypeEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public static RedeemCodeTypeEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (RedeemCodeTypeEnum e : values()) {
            if (e.value == value) {
                return e;
            }
        }
        return null;
    }
}
