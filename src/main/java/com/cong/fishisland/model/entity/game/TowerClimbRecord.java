package com.cong.fishisland.model.entity.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 爬塔挑战记录实体
 *
 * @author cong
 */
@TableName("tower_climb_record")
@Data
public class TowerClimbRecord implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 挑战层数 */
    private Integer floor;

    /** 历史最高通关层数（冗余，方便查询） */
    private Integer maxFloor;

    /** 本次挑战结果：0-失败，1-胜利 */
    private Integer result;

    /** 挑战时宠物等级 */
    private Integer petLevel;

    /** 挑战结束时宠物剩余血量 */
    private Integer petHpLeft;

    /** 本次获得积分奖励 */
    private Integer rewardPoints;

    private Date createTime;
    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}
