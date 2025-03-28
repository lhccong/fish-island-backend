package com.github.yuyuanweb.mianshiyaplugin.model.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 内嵌的网页类型
 *
 * @author pine
 */
@Getter
public enum WebTypeEnum {

    QUESTION("题目", "question"),
    ANSWER("回答", "answer"),
    COMMENT("评论", "comment"),
    ;

    private final String text;

    private final String value;

    WebTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 获取值列表
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 text 获取枚举
     */
    public static WebTypeEnum getEnumByText(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        for (WebTypeEnum item : WebTypeEnum.values()) {
            if (item.text.equals(text)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 根据 value 获取枚举
     */
    public static WebTypeEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (WebTypeEnum item : WebTypeEnum.values()) {
            if (Objects.equals(item.value, value)) {
                return item;
            }
        }
        return null;
    }
}
