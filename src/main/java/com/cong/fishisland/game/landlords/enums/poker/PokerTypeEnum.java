package com.cong.fishisland.game.landlords.enums.poker;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 扑克牌花色枚举
 *
 * @author cong
 */
@Getter
@AllArgsConstructor
public enum PokerTypeEnum {

    SPADE(0, "♠", "spade", "黑桃"),
    HEART(1, "♥", "heart", "红心"),
    CLUB(2, "♣", "club", "梅花"),
    DIAMOND(3, "♦", "diamond", "方块");

    private final int code;
    private final String symbol;
    private final String name;
    private final String chineseName;

    /**
     * 根据 code 获取枚举
     */
    public static PokerTypeEnum getByCode(int code) {
        for (PokerTypeEnum type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }

    /**
     * 获取颜色 (红或黑)
     */
    public boolean isRed() {
        return this == HEART || this == DIAMOND;
    }

    public boolean isBlack() {
        return this == SPADE || this == CLUB;
    }
}
