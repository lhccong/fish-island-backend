package com.cong.fishisland.model.vo.post;

import lombok.Data;

import java.io.Serializable;

/**
 * 帖子兑奖token视图
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Data
public class PostRewardTokenVO implements Serializable {

    /**
     * 兑奖加密token（帖子id + 当前用户id + 盐值加密）
     */
    private String rewardToken;

    private static final long serialVersionUID = 1L;
}




