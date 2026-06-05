package com.cong.fishisland.model.enums.report;

import cn.hutool.core.util.ObjectUtil;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.ReportStatusConstant;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 举报处理状态枚举
 *
 * @author cong
 */
@Getter
public enum ReportStatusEnum {
    PENDING("待处理", ReportStatusConstant.PENDING),
    PROCESSED("已处理", ReportStatusConstant.PROCESSED),
    DISMISSED("已驳回", ReportStatusConstant.DISMISSED);

    private final String text;
    private final Integer value;

    ReportStatusEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    public static ReportStatusEnum getEnumByValue(Integer value) {
        if (ObjectUtil.isEmpty(value)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "举报状态不能为空");
        }
        for (ReportStatusEnum anEnum : ReportStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "举报状态参数不存在，请在：[" +
                Arrays.stream(values()).map(item -> item.value + ":" + item.text)
                        .collect(Collectors.joining(",")) + "]中选择");
    }

    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }
}
