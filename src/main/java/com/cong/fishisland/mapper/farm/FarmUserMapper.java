package com.cong.fishisland.mapper.farm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.farm.FarmUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FarmUserMapper extends BaseMapper<FarmUser> {

    @Update("UPDATE farm_user SET experience = experience + #{exp}, updateTime = NOW() WHERE userId = #{userId}")
    int addExperience(@Param("userId") Long userId, @Param("exp") Integer exp);

    @Update("UPDATE farm_user SET level = #{level}, updateTime = NOW() WHERE userId = #{userId}")
    int updateLevel(@Param("userId") Long userId, @Param("level") Integer level);

    @Update("UPDATE farm_user SET totalHarvest = totalHarvest + 1, updateTime = NOW() WHERE userId = #{userId}")
    int incrementTotalHarvest(@Param("userId") Long userId);

    @Update("UPDATE farm_user SET totalSteal = totalSteal + 1, updateTime = NOW() WHERE userId = #{userId}")
    int incrementTotalSteal(@Param("userId") Long userId);

    @Update("UPDATE farm_user SET totalDefense = totalDefense + 1, updateTime = NOW() WHERE userId = #{userId}")
    int incrementTotalDefense(@Param("userId") Long userId);

    @Update("UPDATE farm_user SET friendCount = friendCount + #{count}, updateTime = NOW() WHERE userId = #{userId}")
    int updateFriendCount(@Param("userId") Long userId, @Param("count") Integer count);

    @Update("UPDATE farm_user SET visitedCount = visitedCount + 1, updateTime = NOW() WHERE userId = #{userId}")
    int incrementVisitedCount(@Param("userId") Long userId);
}
