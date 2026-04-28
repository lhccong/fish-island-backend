package com.cong.fishisland.model.entity.fishbattle;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 摸鱼大乱斗召唤师技能表
 */
@TableName(value = "fish_battle_summoner_spell")
@Data
public class FishBattleSummonerSpell implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 技能唯一标识（如 flash, heal, ignite）
     */
    @TableField("spell_id")
    private String spellId;

    /**
     * 中文名
     */
    private String name;

    /**
     * 图标URL或emoji
     */
    private String icon;

    /**
     * 技能描述
     */
    private String description;

    /**
     * 冷却时间(秒)
     */
    private Integer cooldown;

    /**
     * 技能运行时参数配置(JSON)
     */
    @TableField("asset_config")
    private String assetConfig;

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
