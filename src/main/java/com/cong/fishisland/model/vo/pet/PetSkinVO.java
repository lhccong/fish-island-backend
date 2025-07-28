package com.cong.fishisland.model.vo.pet;

import lombok.Data;
import java.io.Serializable;

/**
 * 宠物皮肤视图对象
 * 
 * @author cong
 */
@Data
public class PetSkinVO implements Serializable {

    /**
     * 皮肤ID
     */
    private Long skinId;
    
    /**
     * 皮肤图片地址
     */
    private String url;
    
    /**
     * 皮肤描述
     */
    private String description;
    
    /**
     * 皮肤名称
     */
    private String name;
    
    /**
     * 皮肤所需兑换积分
     */
    private Integer points;
    
    /**
     * 是否已拥有
     */
    private Boolean owned;

    private static final long serialVersionUID = 1L;
} 