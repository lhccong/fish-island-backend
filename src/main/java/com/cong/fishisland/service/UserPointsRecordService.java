package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.user.UserPointsRecord;

/**
 * @author cong
 * @description 针对表【user_points_record(用户积分记录表)】的数据库操作Service
 * @createDate 2025-03-30
 */
public interface UserPointsRecordService extends IService<UserPointsRecord> {

    /**
     * 添加积分记录
     *
     * @param userId          用户ID
     * @param changeType      变动类型：1-增加，2-扣除
     * @param changePoints    变动积分数量
     * @param beforePoints    变动前总积分
     * @param afterPoints     变动后总积分
     * @param beforeUsedPoints 变动前已用积分
     * @param afterUsedPoints  变动后已用积分
     * @param sourceType      来源类型
     * @param sourceId        来源ID
     * @param description     描述
     * @return 是否成功
     */
    boolean addPointsRecord(Long userId, Integer changeType, Integer changePoints,
                           Integer beforePoints, Integer afterPoints,
                           Integer beforeUsedPoints, Integer afterUsedPoints,
                           String sourceType, String sourceId, String description);

    /**
     * 添加积分增加记录（带积分详情）
     *
     * @param userId           用户ID
     * @param points           增加的积分
     * @param sourceType       来源类型
     * @param description      描述
     * @param beforePoints     变动前总积分
     * @param afterPoints      变动后总积分
     * @param beforeUsedPoints 变动前已用积分
     * @param afterUsedPoints  变动后已用积分
     * @return 是否成功
     */
    boolean addPointsIncreaseRecord(Long userId, Integer points, String sourceType, String description,
                                    Integer beforePoints, Integer afterPoints,
                                    Integer beforeUsedPoints, Integer afterUsedPoints);

    /**
     * 添加积分扣除记录（带积分详情）
     *
     * @param userId           用户ID
     * @param points           扣除的积分
     * @param sourceType       来源类型
     * @param description      描述
     * @param beforePoints     变动前总积分
     * @param afterPoints      变动后总积分
     * @param beforeUsedPoints 变动前已用积分
     * @param afterUsedPoints  变动后已用积分
     * @return 是否成功
     */
    boolean addPointsDeductRecord(Long userId, Integer points, String sourceType, String description,
                                  Integer beforePoints, Integer afterPoints,
                                  Integer beforeUsedPoints, Integer afterUsedPoints);
}
