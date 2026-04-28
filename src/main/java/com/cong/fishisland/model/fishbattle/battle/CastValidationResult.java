package com.cong.fishisland.model.fishbattle.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 施法校验结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CastValidationResult {
    /**
     * 是否通过校验。
     */
    private boolean passed;

    /**
     * 拒绝原因码。
     */
    private String reasonCode;

    /**
     * 拒绝原因描述。
     */
    private String reasonMessage;

    public static CastValidationResult success() {
        return CastValidationResult.builder().passed(true).build();
    }

    public static CastValidationResult fail(String reasonCode, String reasonMessage) {
        return CastValidationResult.builder()
                .passed(false)
                .reasonCode(reasonCode)
                .reasonMessage(reasonMessage)
                .build();
    }
}
