package com.cong.fishisland.model.vo.pet;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 宠物视图对象
 * 
 * @author cong
 */
@Data
public class PetVO implements Serializable {

    /**
     * 宠物ID
     */
    private Long petId;
    
    /**
     * 宠物名称
     */
    private String name;
    
    /**
     * 宠物图片地址
     */
    private String petUrl;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 宠物等级
     */
    private Integer level;
    
    /**
     * 当前经验值
     */
    private Integer exp;
    
    /**
     * 宠物心情值
     */
    private Integer mood;
    
    /**
     * 饥饿度
     */
    private Integer hunger;

    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 宠物拥有的皮肤列表
     */
    private List<PetSkinVO> skins;

    private static final long serialVersionUID = 1L;
} 