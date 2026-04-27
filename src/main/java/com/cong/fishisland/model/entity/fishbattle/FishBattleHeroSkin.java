package com.cong.fishisland.model.entity.fishbattle;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 摸鱼大乱斗英雄皮肤表
 */
@TableName(value = "fish_battle_hero_skin")
@Data
public class FishBattleHeroSkin implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联英雄标识
     */
    @TableField("hero_id")
    private String heroId;

    /**
     * 皮肤唯一标识
     */
    @TableField("skin_id")
    private String skinId;

    /**
     * 皮肤名称
     */
    @TableField("skin_name")
    private String skinName;

    /**
     * 皮肤立绘URL
     */
    @TableField("splash_art")
    private String splashArt;

    /**
     * 皮肤3D模型URL
     */
    @TableField("model_url")
    private String modelUrl;

    /**
     * 是否默认皮肤
     */
    @TableField("is_default")
    private Integer isDefault;

    /**
     * 状态（0禁用/1启用）
     */
    private Integer status;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
