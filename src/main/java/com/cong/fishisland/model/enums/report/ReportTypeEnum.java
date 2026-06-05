package com.cong.fishisland.model.enums.report;

import cn.hutool.core.util.ObjectUtil;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.ReportTypeConstant;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 举报类型枚举
 *
 * @author cong
 */
@Getter
public enum ReportTypeEnum {
    CHAT("聊天记录", ReportTypeConstant.CHAT),
    POST("帖子", ReportTypeConstant.POST),
    MOMENTS("鱼小圈", ReportTypeConstant.MOMENTS);

    private final String text;
    private final Integer value;

    ReportTypeEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    public static ReportTypeEnum getEnumByValue(Integer value) {
        if (ObjectUtil.isEmpty(value)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "举报类型不能为空");
        }
        for (ReportTypeEnum anEnum : ReportTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "举报类型参数不存在，请在：[" +
                Arrays.stream(values()).map(item -> item.value + ":" + item.text)
                        .collect(Collectors.joining(",")) + "]中选择");
    }

    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }
}
