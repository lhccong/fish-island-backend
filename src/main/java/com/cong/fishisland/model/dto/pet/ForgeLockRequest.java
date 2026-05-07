package com.cong.fishisland.model.dto.pet;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 词条锁定/解锁请求
 *
 * @author cong
 */
@Data
@ApiModel(value = "ForgeLockRequest", description = "词条锁定请求，指定需要锁定的词条序号，未在列表中的词条将被解锁")
public class ForgeLockRequest implements Serializable {

    /**
     * 宠物ID
     */
    @ApiModelProperty(value = "宠物ID", required = true, example = "1001")
    private Long petId;

    /**
     * 装备位置 1-武器 2-手套 3-鞋子 4-头盔 5-项链 6-翅膀
     */
    @ApiModelProperty(value = "装备位置：1-武器 2-手套 3-鞋子 4-头盔 5-项链 6-翅膀", required = true, example = "1")
    private Integer equipSlot;

    /**
     * 需要锁定的词条序号列表（1~4），不在列表中的词条将被解锁
     * 传空列表表示解锁全部词条
     */
    @ApiModelProperty(value = "需要锁定的词条序号列表（1~4），不在列表中的词条将被解锁，传空列表表示解锁全部", example = "[1, 3]")
    private List<Integer> lockedEntries;

    private static final long serialVersionUID = 1L;
}
