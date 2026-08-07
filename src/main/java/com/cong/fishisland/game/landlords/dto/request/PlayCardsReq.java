package com.cong.fishisland.game.landlords.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 出牌请求
 *
 * @author cong
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlayCardsReq extends GameActionReq {
    
    /**
     * 出的牌 (牌ID列表)
     */
    private List<String> pokers;
}
