package com.cong.fishisland.model.vo.pet;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 其他用户宠物视图对象（不包含扩展数据）
 * 
 * @author cong
 */
@Data
public class OtherUserPetVO implements Serializable {

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

    private static final long serialVersionUID = 1L;
} 