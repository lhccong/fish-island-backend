package com.cong.fishisland.game.landlords.model.poker;

import com.cong.fishisland.game.landlords.enums.poker.PokerPatternEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 牌型分析结果
 *
 * @author cong
 */
@Getter
@AllArgsConstructor
public class PatternResult {
    
    /**
     * 牌型
     */
    private final PokerPatternEnum pattern;
    
    /**
     * 主面值 (用于比较大小)
     */
    private final int mainValue;
    
    /**
     * 牌数量
     */
    private final int count;
    
    /**
     * 涉及的牌
     */
    private final List<Poker> pokers;
    
    /**
     * 是否有效
     */
    public boolean isValid() {
        return pattern != null && pattern != PokerPatternEnum.INVALID;
    }
}
