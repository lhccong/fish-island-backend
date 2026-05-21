package com.cong.fishisland.model.entity.luckybag;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 福袋实体（仅存 Redis）
 */
@Data
public class LuckyBag implements Serializable {

    private String id;
    private String name;
    private Long creatorId;
    /** 福袋总积分 */
    private Integer totalAmount;
    /** 中奖人数 */
    private Integer winnerCount;
    /** 分配类型：1-随机，2-平均 */
    private Integer type;
    /** 持续秒数 */
    private Integer durationSeconds;
    private Date createTime;
    private Date expireTime;
    /** 状态：0-进行中，1-已开奖，2-已过期（无人参与） */
    private Integer status;
    private Integer participantCount;
    private String creatorName;
    private String creatorAvatar;

    private static final long serialVersionUID = 1L;
}
