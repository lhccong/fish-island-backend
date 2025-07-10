package com.cong.fishisland.model.vo.game;

import com.cong.fishisland.model.enums.RoomStatusEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 你画我猜游戏房间VO
 *
 * @author cong
 */
@Data
public class DrawRoomVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 房间ID
     */
    private String roomId;

    /**
     * 房主ID
     */
    private Long creatorId;

    /**
     * 房主名称
     */
    private String creatorName;

    /**
     * 房主头像
     */
    private String creatorAvatar;

    /**
     * 房间状态
     */
    private RoomStatusEnum status;

    /**
     * 房间最大人数
     */
    private Integer maxPlayers;

    /**
     * 当前房间人数
     */
    private Integer currentPlayers;

    /**
     * 当前词语（仅对房主/画图者可见）
     */
    private String currentWord;

    /**
     * 当前轮次的提示词（所有人可见）
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
     * 当前回合结束时间
     */
    private Long roundEndTime;

    /**
     * 当前绘画者ID
     */
    private Long currentDrawerId;

    /**
     * 当前绘画者名称
     */
    private String currentDrawerName;

    /**
     * 当前绘画数据（Base64编码）
     */
    private String drawData;

    /**
     * 房间参与者列表
     */
    private List<DrawPlayerVO> participants;

    /**
     * 正确猜中的玩家列表
     */
    private List<DrawPlayerVO> correctGuessPlayers;
} 