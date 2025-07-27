package com.cong.fishisland.model.entity.pet;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 宠物皮肤实体
 *
 * @author cong
 */
@TableName(value = "pet_skin")
@Data
public class PetSkin implements Serializable {
    
    /**
     * 皮肤ID
     */
    @TableId(value = "skinId", type = IdType.ASSIGN_ID)
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