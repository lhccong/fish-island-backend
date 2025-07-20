package com.cong.fishisland.model.enums;

import cn.hutool.core.util.ObjectUtil;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.VipTypeConstant;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会员类型枚举
 *
 * @author cong
 */
@Getter
public enum VipTypeEnum {
    MONTHLY("月卡会员", VipTypeConstant.MONTHLY),
    PERMANENT("永久会员", VipTypeConstant.PERMANENT);

    private final String text;
    private final Integer value;

    VipTypeEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static VipTypeEnum getEnumByValue(Integer value) {
        if (ObjectUtil.isEmpty(value)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会员类型枚举不能为空");
        }
        for (VipTypeEnum anEnum : VipTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "会员类型枚举参数不存在，请在：[" + 
                Arrays.stream(values()).map(item -> item.value + ":" + item.text)
                        .collect(Collectors.joining(",")) + "]中选择");
    }

    /**
     * 获取值列表
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }
} 