package com.github.yuyuanweb.mianshiyaplugin.model.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 是否会员专属
 *
 * @author pine
 */
@Getter
public enum NeedVipEnum {

    NO("否", 0),
    YES("是", 1),
    ;

    private final String text;

    private final int value;

    NeedVipEnum(String text, int value) {
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
    public static NeedVipEnum getEnumByText(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        for (NeedVipEnum item : NeedVipEnum.values()) {
            if (item.text.equals(text)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 根据 value 获取枚举
     */
    public static NeedVipEnum getEnumByValue(Integer value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (NeedVipEnum questionDifficultyEnum : NeedVipEnum.values()) {
            if (questionDifficultyEnum.value == value) {
                return questionDifficultyEnum;
            }
        }
        return null;
    }
}
