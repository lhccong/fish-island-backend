package com.cong.fishisland.model.vo.game;

import com.cong.fishisland.model.enums.RoomStatusEnum;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 谁是卧底游戏房间视图对象
 *
 * @author cong
 */
@Data
public class UndercoverRoomVO {

    /**
     * 房间ID
     */
    private String roomId;

    /**
     * 房间状态
     */
    private RoomStatusEnum status;

    /**
     * 玩家词语
     */
    private String word;

    /**
     * 参与者ID列表
     */
    private Set<Long> participantIds;

    /**
     * 参与者详细信息列表
     */
    private List<UndercoverPlayerDetailVO> participants;

    /**
     * 已被淘汰的玩家ID列表
     */
    private Set<Long> eliminatedIds;

    /**
     * 房间创建时间
     */
    private Date createTime;

    /**
     * 游戏开始时间
     */
    private Date startTime;

    /**
     * 游戏持续时间（秒）
     */
    private Integer duration;

    /**
     * 剩余时间（秒）
     */
    private Integer remainingTime;

    /**
     * 投票记录列表
     */
    private List<UndercoverVoteVO> votes;
    
    /**
     * 房间最大人数
     */
    private Integer maxPlayers;
} 