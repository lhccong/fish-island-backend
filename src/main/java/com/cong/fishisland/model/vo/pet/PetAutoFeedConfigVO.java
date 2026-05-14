package com.cong.fishisland.model.vo.pet;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 宠物自动喂食配置 VO
 *
 * @author cong
 */
@Data
public class PetAutoFeedConfigVO implements Serializable {

    /**
     * 配置ID
     */
    private Long id;

    /**
     * 宠物ID
     */
    private Long petId;

    /**
     * 是否启用：0-关闭，1-开启
     */
    private Integer enabled;

    /**
     * 使用的食物模板code
     */
    private String foodCode;

    /**
     * 食物名称（冗余展示）
     */
    private String foodName;

    /**
     * 食物图标
     */
    private String foodIcon;

    /**
     * 触发喂食的饥饿度阈值
     */
    private Integer triggerThreshold;

    /**
     * 当前背包中该食物的剩余数量
     */
    private Integer remainingQuantity;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
