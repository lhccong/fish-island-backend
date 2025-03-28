package com.github.yuyuanweb.mianshiyaplugin.model.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 题目难度枚举
 * 简单 = 1，中等 = 3，困难 = 5
 *
 * @author pine
 */
@Getter
public enum DifficultyEnum {

    SIMPLE("简单", 1),
    MIDDLE("中等", 3),
    HARD("困难", 5),
    ;

    private final String text;

    private final int value;

    DifficultyEnum(String text, int value) {
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
     * 根据 text 获取枚举
     */
    public static DifficultyEnum getEnumByText(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        for (DifficultyEnum item : DifficultyEnum.values()) {
            if (item.text.equals(text)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 根据 value 获取枚举
     */
    public static DifficultyEnum getEnumByValue(Integer value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (DifficultyEnum questionDifficultyEnum : DifficultyEnum.values()) {
            if (questionDifficultyEnum.value == value) {
                return questionDifficultyEnum;
            }
        }
        return null;
    }
}
