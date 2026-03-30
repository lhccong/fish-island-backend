package com.cong.fishisland.service.impl.user;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.user.UserPointsRecordMapper;
import com.cong.fishisland.model.entity.user.UserPointsRecord;
import com.cong.fishisland.service.UserPointsRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author cong
 * @description 针对表【user_points_record(用户积分记录表)】的数据库操作Service实现
 * @createDate 2025-03-30
 */
@Service
@Slf4j
public class UserPointsRecordServiceImpl extends ServiceImpl<UserPointsRecordMapper, UserPointsRecord>
        implements UserPointsRecordService {

    @Override
    public boolean addPointsRecord(Long userId, Integer changeType, Integer changePoints,
                                   Integer beforePoints, Integer afterPoints,
                                   Integer beforeUsedPoints, Integer afterUsedPoints,
                                   String sourceType, String sourceId, String description) {
        try {
            UserPointsRecord record = new UserPointsRecord();
            record.setUserId(userId);
            record.setChangeType(changeType);
            record.setChangePoints(changePoints);
            record.setBeforePoints(beforePoints);
            record.setAfterPoints(afterPoints);
            record.setBeforeUsedPoints(beforeUsedPoints);
            record.setAfterUsedPoints(afterUsedPoints);
            record.setSourceType(sourceType);
            record.setSourceId(sourceId);
            record.setDescription(description);
            return this.save(record);
        } catch (Exception e) {
            log.error("添加积分记录失败，userId={}, changeType={}, changePoints={}", userId, changeType, changePoints, e);
            return false;
        }
    }

    @Override
    public boolean addPointsIncreaseRecord(Long userId, Integer points, String sourceType, String description,
                                           Integer beforePoints, Integer afterPoints,
                                           Integer beforeUsedPoints, Integer afterUsedPoints) {
        return addPointsRecord(userId, 1, points, beforePoints, afterPoints,
                beforeUsedPoints, afterUsedPoints, sourceType, null, description);
    }

    @Override
    public boolean addPointsDeductRecord(Long userId, Integer points, String sourceType, String description,
                                         Integer beforePoints, Integer afterPoints,
                                         Integer beforeUsedPoints, Integer afterUsedPoints) {
        return addPointsRecord(userId, 2, points, beforePoints, afterPoints,
                beforeUsedPoints, afterUsedPoints, sourceType, null, description);
    }
}
