package com.cong.fishisland.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.user.UserPointsRecord;
import lombok.Data;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * @author cong
 * @description 针对表【user_points_record(用户积分记录表)】的数据库操作Mapper
 * @createDate 2025-03-30
 */
public interface UserPointsRecordMapper extends BaseMapper<UserPointsRecord> {

    @Select("SELECT source_type AS sourceType, SUM(change_points) AS earned FROM user_points_record "
            + "WHERE user_id = #{userId} AND change_type = 1 AND change_points > 0 "
            + "AND create_time >= #{start} AND create_time < #{end} GROUP BY source_type")
    List<DailyEarned> dailyEarned(@Param("userId") Long userId, @Param("start") Date start,
                                  @Param("end") Date end);

    @Data
    class DailyEarned {
        private String sourceType;
        private long earned;
    }
}
