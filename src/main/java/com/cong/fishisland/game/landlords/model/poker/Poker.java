package com.cong.fishisland.game.landlords.model.poker;

import com.cong.fishisland.game.landlords.enums.poker.PokerTypeEnum;
import com.cong.fishisland.game.landlords.enums.poker.PokerValueEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * 扑克牌
 *
 * @author cong
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Poker implements Comparable<Poker> {
    
    /**
     * 花色
     */
    private PokerTypeEnum type;
    
    /**
     * 面值
     */
    private PokerValueEnum value;
    
    /**
     * 是否是癞子牌
     */
    private boolean isUniversal;
    
    /**
     * 癞子替代的目标面值 (如果当前是癞子)
     */
    private PokerValueEnum universalTarget;

    /**
     * 是否被选中 (前端用)
     */
    private boolean selected;

    /**
     * 牌组序号（支持多副牌时区分同 ID 的不同实例，默认为 0）
     */
    private int deckIndex = 0;

    public Poker(PokerTypeEnum type, PokerValueEnum value) {
        this.type = type;
        this.value = value;
        this.isUniversal = false;
        this.selected = false;
        this.deckIndex = 0;
    }

    /**
     * 创建癞子牌
     */
    public static Poker createUniversal(PokerValueEnum value) {
        Poker poker = new Poker(null, value);
        poker.setUniversal(true);
        return poker;
    }

    /**
     * 创建大小王
     */
    public static Poker createJoker(boolean isBig) {
        return new Poker(null, isBig ? PokerValueEnum.BIG_JOKER : PokerValueEnum.SMALL_JOKER);
    }

    /**
     * 获取唯一标识
     * 癞子牌会在前面加 '*'，方便前后端识别
     * deckIndex > 0 时追加 "#序号"，支持多副牌场景下区分同 ID 的不同实例
     */
    public String getId() {
        StringBuilder sb = new StringBuilder();
        if (isUniversal) {
            sb.append('*');
        }
        if (type == null) {
            sb.append(value.getAlias());
        } else {
            sb.append(type.getSymbol()).append(value.getAlias());
        }
        if (deckIndex > 0) {
            sb.append('#').append(deckIndex);
        }
        return sb.toString();
    }

    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        if (type == null) {
            return value.getDisplay();
        }
        String color = type.isRed() ? "red" : "black";
        return String.format("[%s%s]", type.getSymbol(), value.getDisplay());
    }

    /**
     * 获取排序值 (用于手牌排序)
     */
    public int getSortValue() {
        int base = value.getValue();
        if (isUniversal) {
            base = 100;
        }
        return base;
    }

    /**
     * 获取斗地主中的排序值
     */
    public int getLandlordsSortValue() {
        if (value == PokerValueEnum.SMALL_JOKER) {
            return 16;
        } else if (value == PokerValueEnum.BIG_JOKER) {
            return 17;
        } else if (value == PokerValueEnum.TWO) {
            return 15;
        } else if (value == PokerValueEnum.ACE) {
            return 14;
        } else {
            return value.getValue();
        }
    }

    @Override
    public int compareTo(Poker o) {
        if (o == null) {
            return -1;
        }
        return this.getLandlordsSortValue() - o.getLandlordsSortValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof Poker)) return false;
        Poker poker = (Poker) o;
        if (this.isUniversal && poker.isUniversal) {
            return this.universalTarget == poker.universalTarget;
        }
        if (this.isUniversal) {
            return this.universalTarget == poker.value;
        }
        if (poker.isUniversal) {
            return this.value == poker.universalTarget;
        }
        return type == poker.type && value == poker.value && deckIndex == poker.deckIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value, isUniversal, universalTarget, deckIndex);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
