package com.cong.fishisland.model.dto.pet;

import lombok.Data;

import java.io.Serializable;

/**
 * 装备升级请求
 *
 * @author cong
 */
@Data
public class ForgeUpgradeRequest implements Serializable {

    /**
     * 宠物ID
     */
    private Long petId;

    /**
     * 装备位置 1-武器 2-手套 3-鞋子 4-头盔 5-项链 6-翅膀
     * 武器不支持升级
     */
    private Integer equipSlot;

    private static final long serialVersionUID = 1L;
}
