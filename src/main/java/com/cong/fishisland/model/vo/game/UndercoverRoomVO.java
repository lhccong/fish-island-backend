package com.cong.fishisland.model.vo.game;

import com.cong.fishisland.model.entity.game.UndercoverRoom;
import lombok.Data;

import java.util.Date;
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
    private UndercoverRoom.RoomStatus status;

    /**
     * 参与者ID列表
     */
    private Set<Long> participantIds;

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
} 