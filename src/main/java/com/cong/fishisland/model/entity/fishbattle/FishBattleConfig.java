package com.cong.fishisland.model.entity.fishbattle;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 摸鱼大乱斗游戏配置表
 */
@Data
@TableName("fish_battle_config")
public class FishBattleConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配置唯一标识（如 map_default / game_default）
     */
    @TableField("config_key")
    private String configKey;

    /**
     * 配置数据（JSON格式）
     */
    @TableField("config_data")
    private String configData;

    /**
     * 配置说明
     */
    @TableField("description")
    private String description;

    /**
     * 状态（0禁用/1启用）
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private Date updateTime;
}
