package com.cong.fishisland.model.dto.item;

import lombok.Data;

import java.io.Serializable;

/**
 * 装备穿戴请求
 *
 * @author cong
 */
@Data
public class ItemEquipRequest implements Serializable {

    /**
     * 物品实例ID
     */
    private Long itemInstanceId;

    private static final long serialVersionUID = 1L;
}
