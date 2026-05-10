package com.cong.fishisland.model.enums.pay;

import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import lombok.Getter;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 支付订单类型枚举
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Getter
public enum PayOrderTypeEnum {

    SPONSOR("赞助摸鱼岛", 1);

    private final String text;
    private final Integer value;

    PayOrderTypeEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static PayOrderTypeEnum getEnumByValue(Integer value) {
        if (value == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "支付类型不能为空");
        }
        for (PayOrderTypeEnum item : PayOrderTypeEnum.values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR,
                "不支持的支付类型，请在 [" +
                Arrays.stream(values())
                        .map(e -> e.value + ":" + e.text)
                        .collect(Collectors.joining(", ")) +
                "] 中选择");
    }
}
