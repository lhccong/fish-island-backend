package com.cong.fishisland.model.entity.pet;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 摸鱼宠物实体
 *
 * @author cong
 */
@TableName(value = "fish_pet")
@Data
public class FishPet implements Serializable {
    
    /**
     * 宠物ID
     */
    @TableId(value = "petId", type = IdType.ASSIGN_ID)
    private Long petId;
    
    /**
     * 宠物图片地址
     */
    private String petUrl;
    
    /**
     * 宠物名称
     */
    private String name;
    
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
     * 宠物心情值（0-100）
     */
    private Integer mood;
    
    /**
     * 饥饿度（越高越饿，建议范围 0-100）
     */
    private Integer hunger;
    
    /**
     * 宠物扩展数据（技能、形象等，JSON格式）
     */
    private String extendData;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
    
    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;
    
    private static final long serialVersionUID = 1L;
} 