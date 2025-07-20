package com.cong.fishisland.model.entity.game;

import com.cong.fishisland.model.enums.RoomStatusEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

/**
 * 你画我猜房间实体
 */
@Data
public class DrawRoom implements Serializable {
    /**
     * 房间状态
     */
    private RoomStatusEnum status;

    /**
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 参与者ID列表
     */
    private Set<Long> participantIds;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 最大玩家数
     */
    private Integer maxPlayers;

    /**
     * 总轮数
     */
    private Integer totalRounds;

    /**
     * 当前轮数
     */
    private Integer currentRound;

    /**
     * 轮次持续时间（秒）
     */
    private Integer roundDuration;

    /**
     * 轮次结束时间戳（秒）
     */
    private Long roundEndTime;

    /**
     * 当前词语
     */
    private String currentWord;

    /**
     * 词语提示
     */
    private String wordHint;

    /**
     * 当前绘画者ID
     */
    private Long currentDrawerId;

    /**
     * 已猜中的用户ID列表
     */
    private Set<Long> correctGuessIds;

    /**
     * 是否仅创建者绘画模式
     * true: 房主绘画模式
     * false: 轮换模式
     */
    private Boolean creatorOnlyMode;
    
    /**
     * 词库类型
     */
    private String wordType;

    private static final long serialVersionUID = 1L;
} 