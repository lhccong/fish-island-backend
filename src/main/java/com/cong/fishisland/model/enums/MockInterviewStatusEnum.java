package com.cong.fishisland.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模拟面试状态枚举
 *
 * @author shing
 */
@Getter
public enum MockInterviewStatusEnum {

    TO_START("待开始", 0),
    IN_PROGRESS("进行中", 1),
    ENDED("已结束", 2);

    private final String text;

    private final int value;

    MockInterviewStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 获取值列表
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     */
    public static MockInterviewStatusEnum getEnumByValue(Integer value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (MockInterviewStatusEnum anEnum : MockInterviewStatusEnum.values()) {
            if (anEnum.value == value) {
                return anEnum;
            }
        }
        return null;
    }
}
