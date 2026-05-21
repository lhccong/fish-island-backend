package com.cong.fishisland.model.entity.luckybag;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 福袋中奖记录（仅存 Redis）
 */
@Data
public class LuckyBagRecord implements Serializable {

    private String id;
    private String luckyBagId;
    private Long userId;
    private Integer amount;
    private Date winTime;

    private static final long serialVersionUID = 1L;
}
