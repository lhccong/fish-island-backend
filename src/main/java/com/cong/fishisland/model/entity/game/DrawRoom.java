package com.cong.fishisland.model.entity.game;

import com.cong.fishisland.model.enums.RoomStatusEnum;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 你画我猜游戏房间实体
 *
 * @author cong
 */
@Data
public class DrawRoom {

    /**
     * 房主ID
     */
    private Long creatorId;

    /**
     * 房间状态
     */
    private RoomStatusEnum status;

    /**
     * 参与者ID集合
     */
    private Set<Long> participantIds;

    /**
     * 房间最大人数
     */
    private Integer maxPlayers;

    /**
     * 当前轮次的词语
     */
    private String currentWord;

    /**
     * 当前轮次的提示词（如：动物、物品、动词等）
     */
    private String wordHint;

    /**
     * 房间创建时间
     */
    private Date createTime;

    /**
     * 游戏开始时间
     */
    private Date startTime;

    /**
     * 当前回合结束时间（秒）
     */
    private Long roundEndTime;

    /**
     * 总共轮数
     */
    private Integer totalRounds;
    
    /**
     * 每轮游戏持续时间（秒）
     */
    private Integer roundDuration;

    /**
     * 当前正确猜中的玩家ID列表
     */
    private Set<Long> correctGuessIds;

    /**
     * 当前绘画者ID
     */
    private Long currentDrawerId;
    
    /**
     * 当前轮次
     */
    private Integer currentRound;
    
    /**
     * 房间模式
     * true: 房主绘画模式
     * false: 轮换模式
     */
    private Boolean creatorOnlyMode;
} 