package com.cong.fishisland.constant;

/**
 * 动作类型
 */
public interface ActionTypeConstant {
    /**
     * 点赞
     */
    String LIKE = "like";

    /**
     * @提及
     */
    String AT = "at";

    /**
     * 回复
     */
    String REPLY = "reply";

    /**
     * 评论
     */
    String COMMENT = "comment";

    /**
     * 关注
     */
    String FOLLOW = "follow";

    /**
     * 分享
     */
    String SHARE = "share";

    /**
     * 系统
     */
    String SYSTEM = "system";

    /** 脚本检测告警，sourceId 是被检测的用户。 */
    String SCRIPT_DETECTION = "script_detection";

    /**
     * 打赏
     */
    String REWARD = "reward";

    /**
     * 农场偷菜
     */
    String FARM_STEAL = "farm_steal";

    /**
     * 福袋中奖
     */
    String LUCKY_BAG_WIN = "lucky_bag_win";
}
