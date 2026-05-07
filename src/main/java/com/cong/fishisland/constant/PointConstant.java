package com.cong.fishisland.constant;

/**
 * 积分常量
 */
public interface PointConstant {

    /**
     * 修改名字积分
     */
    Integer RENAME_POINT = 100;
    /**
     * 签到积分
     */
    Integer SIGN_IN_POINT = 10;
    /**
     * 发言积分
     */
    Integer SPEAK_POINT = 1;

    /**
     * 发布朋友圈积分
     */
    Integer MOMENTS_PUBLISH_POINT = 5;

    /**
     * 朋友圈点赞积分
     */
    Integer MOMENTS_LIKE_POINT = 1;

    /**
     * 每日朋友圈点赞积分上限
     */
    Integer MAX_DAILY_MOMENTS_LIKE_POINTS = 5;

    /**
     * 朋友圈打赏最小积分
     */
    Integer MOMENTS_REWARD_MIN_POINT = 1;

    /**
     * 朋友圈打赏最大积分
     */
    Integer MOMENTS_REWARD_MAX_POINT = 20;
}
