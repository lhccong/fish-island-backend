package com.cong.fishisland.service;

import com.cong.fishisland.model.entity.user.UserPoints;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author cong
* @description 针对表【user_points(用户积分)】的数据库操作Service
* @createDate 2025-03-12 16:13:45
*/
public interface UserPointsService extends IService<UserPoints> {

    boolean signIn();

    void updatePoints(Long userId, Integer points, boolean isSignIn);

    void updateUsedPoints(Long userId, Integer points);

    void addSpeakPoint(Long userId);

    void deductPoints(Long userId, Integer pointsToDeduct);

    /**
     * 扣除积分（带来源信息）
     *
     * @param userId         用户ID
     * @param pointsToDeduct 要扣除的积分
     * @param sourceType     来源类型
     * @param sourceId       来源ID
     * @param description    描述
     */
    void deductPoints(Long userId, Integer pointsToDeduct, String sourceType, String sourceId, String description);

    /**
     * 更新已用积分（带来源信息）
     *
     * @param userId      用户ID
     * @param points      积分变动（负数表示返还）
     * @param sourceType  来源类型
     * @param sourceId    来源ID
     * @param description 描述
     */
    void updateUsedPoints(Long userId, Integer points, String sourceType, String sourceId, String description);

    /**
     * 校验用户可用积分是否充足（可用积分 = points - usedPoints），不足则抛出异常
     *
     * @param userId         用户ID
     * @param requiredPoints 需要的积分数量
     */
    void checkAvailablePoints(Long userId, Integer requiredPoints);
}
