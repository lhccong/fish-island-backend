package com.cong.fishisland.model.enums.redpacket;

import lombok.Getter;

/**
 * 红包类型枚举
 */
@Getter
public enum RedPacketTypeEnum {

    RANDOM(1, "随机红包"),
    AVERAGE(2, "平均红包"),
    QUIZ(3, "答题红包");

    private final int value;
    private final String text;

    RedPacketTypeEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public static RedPacketTypeEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (RedPacketTypeEnum e : values()) {
            if (e.value == value) {
                return e;
            }
        }
        return null;
    }
}
