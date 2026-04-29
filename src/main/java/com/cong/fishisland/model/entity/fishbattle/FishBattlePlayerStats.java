package com.cong.fishisland.model.entity.fishbattle;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 摸鱼大乱斗玩家对局统计表（单局）
 */
@TableName(value = "fish_battle_player_stats")
@Data
public class FishBattlePlayerStats implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 对局ID
     */
    @TableField("game_id")
    private Long gameId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 使用的英雄ID
     */
    @TableField("hero_id")
    private String heroId;

    /**
     * 所在队伍（blue/red）
     */
    private String team;

    /**
     * 击杀数
     */
    private Integer kills;

    /**
     * 死亡数
     */
    private Integer deaths;

    /**
     * 助攻数
     */
    private Integer assists;

    /**
     * 输出伤害
     */
    @TableField("damage_dealt")
    private Integer damageDealt;

    /**
     * 承受伤害
     */
    @TableField("damage_taken")
    private Integer damageTaken;

    /**
     * 治疗量
     */
    private Integer healing;

    /**
     * 是否MVP（0否/1是）
     */
    @TableField("is_mvp")
    private Integer isMvp;

    /**
     * 是否胜利（0否/1是）
     */
    @TableField("is_win")
    private Integer isWin;

    /**
     * 获赞数
     */
    private Integer likes;

    /**
     * 本局获得积分
     */
    @TableField("points_earned")
    private Integer pointsEarned;

    @TableField("create_time")
    private Date createTime;

    /** 英雄中文名（非持久化，API 返回时填充） */
    @TableField(exist = false)
    private String heroName;

    /** 玩家昵称（非持久化，API 返回时填充） */
    @TableField(exist = false)
    private String playerName;

    /** 玩家头像（非持久化，API 返回时填充） */
    @TableField(exist = false)
    private String userAvatar;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
