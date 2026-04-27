package com.cong.fishisland.model.entity.fishbattle;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 摸鱼大乱斗英雄表
 */
@TableName(value = "fish_battle_hero")
@Data
public class FishBattleHero implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 英雄唯一标识（如 yasuo）
     */
    @TableField("hero_id")
    private String heroId;

    /**
     * 中文名
     */
    private String name;

    /**
     * 英文名
     */
    @TableField("name_en")
    private String nameEn;

    /**
     * 职业类型（tank/fighter/mage/marksman/support）
     */
    private String role;

    /**
     * 基础生命值
     */
    @TableField("base_hp")
    private Integer baseHp;

    /**
     * 基础法力值
     */
    @TableField("base_mp")
    private Integer baseMp;

    /**
     * 基础物理攻击力
     */
    @TableField("base_ad")
    private Integer baseAd;

    /**
     * 基础移动速度
     */
    @TableField("move_speed")
    private Integer moveSpeed;

    /**
     * 基础攻击距离
     */
    @TableField("attack_range")
    private BigDecimal attackRange;

    /**
     * 基础攻击速度
     */
    @TableField("attack_speed")
    private BigDecimal attackSpeed;

    /**
     * 英雄头像URL
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 英雄立绘URL（选英雄界面大图展示用）
     */
    @TableField("splash_art")
    private String splashArt;

    /**
     * 3D模型URL
     */
    @TableField("model_url")
    private String modelUrl;

    @TableField("asset_config")
    private String assetConfig;

    /**
     * 技能JSON（Q/W/E/R名称+图标+描述）
     */
    private String skills;

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
