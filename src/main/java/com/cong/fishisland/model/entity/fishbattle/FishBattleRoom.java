package com.cong.fishisland.model.entity.fishbattle;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 摸鱼大乱斗游戏房间表
 */
@TableName(value = "fish_battle_room")
@Data
public class FishBattleRoom implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 房间编码（展示用/邀请码）
     */
    @TableField("room_code")
    private String roomCode;

    /**
     * 房间名称
     */
    @TableField("room_name")
    private String roomName;

    /**
     * 房间状态（0等待中/1选英雄中/2对局中/3已结束）
     */
    private Integer status;

    /**
     * 游戏模式
     */
    @TableField("game_mode")
    private String gameMode;

    /**
     * 最大人数
     */
    @TableField("max_players")
    private Integer maxPlayers;

    /**
     * 当前人数
     */
    @TableField("current_players")
    private Integer currentPlayers;

    /**
     * 是否开启AI补位（0否/1是）
     */
    @TableField("ai_fill_enabled")
    private Integer aiFillEnabled;

    /**
     * 创建者用户ID
     */
    @TableField("creator_id")
    private Long creatorId;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableLogic
    @TableField("is_delete")
    private Integer isDelete;

    /**
     * 创建者用户名（非DB字段，查询后填充）
     */
    @TableField(exist = false)
    private String creatorName;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
