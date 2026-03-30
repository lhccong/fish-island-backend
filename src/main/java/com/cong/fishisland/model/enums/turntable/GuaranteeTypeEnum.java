package com.cong.fishisland.model.enums.turntable;

import lombok.Getter;

/**
 * 保底类型枚举
 * @author cong
 */
@Getter
public enum GuaranteeTypeEnum {

    NONE(0, "无保底"),
    SMALL(1, "小保底"),
    BIG(2, "大保底");

    private final Integer value;
    private final String text;

    GuaranteeTypeEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     */
    public static GuaranteeTypeEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (GuaranteeTypeEnum anEnum : GuaranteeTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
