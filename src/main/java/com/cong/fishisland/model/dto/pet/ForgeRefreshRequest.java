package com.cong.fishisland.model.dto.pet;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 刷新装备词条请求
 *
 * @author cong
 */
@Data
public class ForgeRefreshRequest implements Serializable {

    /**
     * 宠物ID
     */
    private Long petId;

    /**
     * 装备位置 1-武器 2-手套 3-鞋子 4-头盔 5-项链 6-翅膀
     */
    private Integer equipSlot;

    /**
     * 需要锁定的词条序号列表（1~4），锁定的词条不会被刷新
     * 每锁定一条额外消耗 50 积分
     */
    private List<Integer> lockedEntries;

    private static final long serialVersionUID = 1L;
}
