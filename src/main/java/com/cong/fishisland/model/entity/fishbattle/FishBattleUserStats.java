package com.cong.fishisland.model.entity.fishbattle;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 摸鱼大乱斗玩家总体统计表
 */
@TableName(value = "fish_battle_user_stats")
@Data
public class FishBattleUserStats implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 总场次
     */
    @TableField("total_games")
    private Integer totalGames;

    /**
     * 胜场
     */
    private Integer wins;

    /**
     * 败场
     */
    private Integer losses;

    /**
     * 总击杀
     */
    @TableField("total_kills")
    private Integer totalKills;

    /**
     * 总死亡
     */
    @TableField("total_deaths")
    private Integer totalDeaths;

    /**
     * 总助攻
     */
    @TableField("total_assists")
    private Integer totalAssists;

    /**
     * MVP次数
     */
    @TableField("mvp_count")
    private Integer mvpCount;

    /**
     * 当前连胜（负数表示连败）
     */
    @TableField("current_streak")
    private Integer currentStreak;

    /**
     * 最大连胜
     */
    @TableField("max_streak")
    private Integer maxStreak;

    /**
     * 今日已玩场次
     */
    @TableField("today_games")
    private Integer todayGames;

    /**
     * 今日日期（用于每日重置计数）
     */
    @TableField("today_date")
    private Date todayDate;

    /**
     * 每日对局上限
     */
    @TableField("daily_limit")
    private Integer dailyLimit;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
