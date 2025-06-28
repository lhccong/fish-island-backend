package com.cong.fishisland.model.vo.game;

import lombok.Data;

/**
 * 谁是卧底游戏玩家详细信息视图对象
 *
 * @author cong
 */
@Data
public class UndercoverPlayerDetailVO {

    /**
     * 玩家ID
     */
    private Long userId;
    
    /**
     * 玩家用户名
     */
    private String userName;
    
    /**
     * 玩家头像
     */
    private String userAvatar;


    /**
     * 是否被淘汰
     */
    private Boolean isEliminated;
    
    /**
     * 收到的票数
     */
    private Integer voteCount;
} 