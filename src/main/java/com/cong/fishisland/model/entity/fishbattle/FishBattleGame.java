package com.cong.fishisland.model.entity.fishbattle;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 摸鱼大乱斗对局记录表
 */
@TableName(value = "fish_battle_game")
@Data
public class FishBattleGame implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联房间ID
     */
    @TableField("room_id")
    private Long roomId;

    /**
     * 游戏模式
     */
    @TableField("game_mode")
    private String gameMode;

    /**
     * 胜利队伍（blue/red）
     */
    @TableField("winning_team")
    private String winningTeam;

    /**
     * 蓝队总击杀
     */
    @TableField("blue_kills")
    private Integer blueKills;

    /**
     * 红队总击杀
     */
    @TableField("red_kills")
    private Integer redKills;

    /**
     * 对局时长（秒）
     */
    @TableField("duration_seconds")
    private Integer durationSeconds;

    /**
     * 结束原因（crystal_destroyed/kill_limit）
     */
    @TableField("end_reason")
    private String endReason;

    /**
     * MVP用户ID
     */
    @TableField("mvp_user_id")
    private Long mvpUserId;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private Date startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    private Date endTime;

    @TableField("create_time")
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
