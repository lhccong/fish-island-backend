package com.cong.fishisland.model.dto.pet;

import lombok.Data;

import java.io.Serializable;

/**
 * 宠物自动喂食配置请求（新增/更新通用）
 *
 * @author cong
 */
@Data
public class PetAutoFeedConfigRequest implements Serializable {

    /**
     * 宠物ID
     */
    private Long petId;

    /**
     * 是否启用：0-关闭，1-开启
     */
    private Integer enabled;

    /**
     * 使用的食物模板code，关联 item_templates.code
     */
    private String foodCode;

    /**
     * 触发喂食的饱食度阈值（低于此值时自动喂食，范围 1-100）
     * hunger 越高越饱，低于此阈值说明宠物饿了
     */
    private Integer triggerThreshold;

    private static final long serialVersionUID = 1L;
}
