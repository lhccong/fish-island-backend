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
}
