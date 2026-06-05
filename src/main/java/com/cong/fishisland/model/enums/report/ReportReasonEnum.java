package com.cong.fishisland.model.enums.report;

import cn.hutool.core.util.ObjectUtil;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.ReportReasonConstant;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 举报原因枚举
 *
 * @author cong
 */
@Getter
public enum ReportReasonEnum {
    UNRELATED_COMMUNITY("发布社区无关内容", ReportReasonConstant.UNRELATED_COMMUNITY),
    ABUSE_PROVOKE("发布辱骂/引战内容", ReportReasonConstant.ABUSE_PROVOKE),
    ILLEGAL_TRADE("发布违规交易内容", ReportReasonConstant.ILLEGAL_TRADE),
    RUMOR("发布未证实/谣传内容", ReportReasonConstant.RUMOR),
    COMMUNITY_RULE("发布违反社区规则内容", ReportReasonConstant.COMMUNITY_RULE),
    INFRINGEMENT("发布侵权/抄袭内容", ReportReasonConstant.INFRINGEMENT),
    MALICIOUS_SPOILER("发布恶意剧透内容", ReportReasonConstant.MALICIOUS_SPOILER),
    PRIVACY("发布侵犯隐私内容", ReportReasonConstant.PRIVACY),
    LOTTERY_VIOLATION("抽奖内容违规", ReportReasonConstant.LOTTERY_VIOLATION),
    MEANINGLESS("发布无意义内容", ReportReasonConstant.MEANINGLESS),
    PORNOGRAPHIC("发布色情低俗内容", ReportReasonConstant.PORNOGRAPHIC),
    ILLEGAL_AD("发布违规广告内容", ReportReasonConstant.ILLEGAL_AD),
    ILLEGAL_POLICY("发布违反法规/政策内容", ReportReasonConstant.ILLEGAL_POLICY),
    PIRACY_CHEAT("发布盗版/游戏作弊内容", ReportReasonConstant.PIRACY_CHEAT),
    FRAUD("发布欺诈内容", ReportReasonConstant.FRAUD),
    DANGEROUS("发布危险/引人不适内容", ReportReasonConstant.DANGEROUS),
    HARM_MINORS("发布危害未成年人内容", ReportReasonConstant.HARM_MINORS),
    AI_CONTENT("AI生成内容问题", ReportReasonConstant.AI_CONTENT);

    private final String text;
    private final Integer value;

    ReportReasonEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    public static ReportReasonEnum getEnumByValue(Integer value) {
        if (ObjectUtil.isEmpty(value)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "举报原因不能为空");
        }
        for (ReportReasonEnum anEnum : ReportReasonEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "举报原因参数不存在，请在：[" +
                Arrays.stream(values()).map(item -> item.value + ":" + item.text)
                        .collect(Collectors.joining(",")) + "]中选择");
    }

    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }
}
