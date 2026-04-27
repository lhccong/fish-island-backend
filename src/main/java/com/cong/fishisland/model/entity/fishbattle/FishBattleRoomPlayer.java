package com.cong.fishisland.model.entity.fishbattle;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 摸鱼大乱斗房间玩家表
 */
@TableName(value = "fish_battle_room_player")
@Data
public class FishBattleRoomPlayer implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 房间ID
     */
    @TableField("room_id")
    private Long roomId;

    /**
     * 用户ID（AI玩家为NULL）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 玩家名称
     */
    @TableField("player_name")
    private String playerName;

    /**
     * 队伍（blue/red）
     */
    private String team;

    /**
     * 是否准备（0否/1是）
     */
    @TableField("is_ready")
    private Integer isReady;

    /**
     * 是否为AI（0否/1是）
     */
    @TableField("is_ai")
    private Integer isAi;

    /**
     * 分配的英雄ID
     */
    @TableField("hero_id")
    private String heroId;

    /**
     * 选择的皮肤ID
     */
    @TableField("skin_id")
    private String skinId;

    /**
     * 召唤师技能1
     */
    private String spell1;

    /**
     * 召唤师技能2
     */
    private String spell2;

    /**
     * 队伍中的位置索引（0-4）
     */
    @TableField("slot_index")
    private Integer slotIndex;

    @TableField("create_time")
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
