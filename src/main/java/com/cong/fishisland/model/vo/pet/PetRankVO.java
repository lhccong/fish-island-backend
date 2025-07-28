package com.cong.fishisland.model.vo.pet;

import lombok.Data;
import java.io.Serializable;

/**
 * 宠物排行榜视图对象
 * 
 * @author cong
 */
@Data
public class PetRankVO implements Serializable {

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
     * 宠物等级
     */
    private Integer level;
    
    /**
     * 当前经验值
     */
    private Integer exp;
    
    /**
     * 宠物主人ID
     */
    private Long userId;
    
    /**
     * 宠物主人昵称
     */
    private String userName;
    
    /**
     * 宠物主人头像
     */
    private String userAvatar;
    
    /**
     * 排名
     */
    private Integer rank;

    private static final long serialVersionUID = 1L;
} 