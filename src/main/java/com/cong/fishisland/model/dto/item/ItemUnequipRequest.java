package com.cong.fishisland.model.dto.item;

import lombok.Data;

import java.io.Serializable;

/**
 * 卸下装备请求
 *
 * @author cong
 */
@Data
public class ItemUnequipRequest implements Serializable {

    /**
     * 装备槽位 (head-头部, hand-手部, foot-脚部, weapon-武器)
     */
    private String equipSlot;

    private static final long serialVersionUID = 1L;
}
