package com.cong.fishisland.game.landlords.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 叫地主请求
 *
 * @author cong
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RobLandlordReq extends GameActionReq {
    
    /**
     * 叫分动作
     * 0 - 不叫
     * 1 - 叫1分
     * 2 - 叫2分
     * 3 - 叫3分
     */
    private Integer action;
}
