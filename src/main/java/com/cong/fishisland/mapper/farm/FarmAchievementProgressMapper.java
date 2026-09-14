package com.cong.fishisland.mapper.farm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.farm.FarmAchievementProgress;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FarmAchievementProgressMapper extends BaseMapper<FarmAchievementProgress> {

    /**
     * 进度插入或推进：已领取（2）的记录不再回退；非已完成状态提升为已完成时记录达成时间。
     *
     * @return 1-新增或状态推进，2-命中唯一键但状态未变化
     */
    @Insert("INSERT INTO farm_achievement_progress (userId, taskId, status, completedTime, createTime) "
            + "VALUES (#{userId}, #{taskId}, #{status}, "
            + "CASE WHEN #{status} >= 1 THEN NOW() ELSE NULL END, NOW()) "
            + "ON DUPLICATE KEY UPDATE "
            + "status = IF(status >= #{status}, status, #{status}), "
            + "completedTime = IF(completedTime IS NULL AND #{status} >= 1, NOW(), completedTime)")
    int upsertProgress(@Param("userId") Long userId,
                       @Param("taskId") Long taskId,
                       @Param("status") Integer status);

    /**
     * 领取奖励：仅"已完成未领取(1)"可领取，返回影响行数用于防重复领取。
     */
    @Update("UPDATE farm_achievement_progress SET status = 2, claimedTime = NOW(), updateTime = NOW() "
            + "WHERE userId = #{userId} AND taskId = #{taskId} AND status = 1")
    int markClaimed(@Param("userId") Long userId, @Param("taskId") Long taskId);
}
