package com.cong.fishisland.model.entity.game;

import com.cong.fishisland.model.enums.RoomStatusEnum;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 谁是卧底游戏房间
 *
 * @author cong
 */
@Data
public class UndercoverRoom {


    /**
     * 房间状态
     */
    private RoomStatusEnum status;

    /**
     * 参与者ID列表
     */
    private Set<Long> participantIds;
    
    /**
     * 有序的参与者ID列表（保存打乱后的玩家顺序）
     */
    private List<Long> orderedParticipantIds = new ArrayList<>();

    /**
     * 卧底玩家ID列表
     */
    private Set<Long> undercoverIds;

    /**
     * 平民玩家ID列表
     */
    private Set<Long> civilianIds;

    /**
     * 平民词语
     */
    private String civilianWord;

    /**
     * 卧底词语
     */
    private String undercoverWord;

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
     * 创建者ID（管理员）
     */
    private Long creatorId;
    
    /**
     * 房间最大人数
     */
    private Integer maxPlayers;
    
    /**
     * 游戏模式：1-常规模式(默认)，2-卧底猜词模式
     */
    private Integer gameMode = 1;
    
    /**
     * 卧底猜词次数记录，key为卧底用户ID，value为已猜词次数
     */
    private Map<Long, Integer> guessCountMap = new HashMap<>();
} 