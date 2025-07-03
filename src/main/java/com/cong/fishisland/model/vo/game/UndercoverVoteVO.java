package com.cong.fishisland.model.vo.game;

import lombok.Data;

import java.util.Date;

/**
 * 谁是卧底游戏投票记录视图对象
 *
 * @author cong
 */
@Data
public class UndercoverVoteVO {

    /**
     * 投票者ID
     */
    private Long voterId;
    
    /**
     * 投票者用户名
     */
    private String voterName;
    
    /**
     * 投票者头像
     */
    private String voterAvatar;
    
    /**
     * 被投票者ID
     */
    private Long targetId;
    
    /**
     * 被投票者用户名
     */
    private String targetName;
    
    /**
     * 被投票者头像
     */
    private String targetAvatar;
    
    /**
     * 投票时间
     */
    private Date voteTime;
} 