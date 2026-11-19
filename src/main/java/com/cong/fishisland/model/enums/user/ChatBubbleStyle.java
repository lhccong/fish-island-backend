package com.cong.fishisland.model.enums.user;

import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatBubbleStyle {
    TIDE_LETTER("tide-letter", "潮汐来信"),
    BERRY_CAT_PAW("berry-cat-paw", "草莓猫爪"),
    MOON_JELLYFISH("moon-jellyfish", "月光水母"),
    YUZU_CAPYBARA("yuzu-capybara", "柚子水豚"),
    SEA_SALT_SEAL("sea-salt-seal", "海盐小海豹"),
    BLACK_SUGAR_JELLY("black-sugar-jelly", "黑糖星冻");

    private final String id;
    private final String displayName;

    public static ChatBubbleStyle require(String id) {
        for (ChatBubbleStyle style : values()) {
            if (style.id.equals(id)) {
                return style;
            }
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择有效的聊天气泡");
    }
}
