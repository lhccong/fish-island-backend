package com.cong.fishisland.mapper.farm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.farm.FarmUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FarmUserMapper extends BaseMapper<FarmUser> {

    @Update("UPDATE farm_user SET experience = experience + #{exp}, updatedAt = NOW() WHERE id = #{userId}")
    int addExperience(@Param("userId") Long userId, @Param("exp") Integer exp);

    @Update("UPDATE farm_user SET coin = coin + #{coin}, updatedAt = NOW() WHERE id = #{userId}")
    int addCoin(@Param("userId") Long userId, @Param("coin") Integer coin);

    @Update("UPDATE farm_user SET level = #{level}, updatedAt = NOW() WHERE id = #{userId}")
    int updateLevel(@Param("userId") Long userId, @Param("level") Integer level);

    @Update("UPDATE farm_user SET totalHarvest = totalHarvest + 1, updatedAt = NOW() WHERE id = #{userId}")
    int incrementTotalHarvest(@Param("userId") Long userId);

    @Update("UPDATE farm_user SET totalSteal = totalSteal + 1, updatedAt = NOW() WHERE id = #{userId}")
    int incrementTotalSteal(@Param("userId") Long userId);

    @Update("UPDATE farm_user SET totalDefense = totalDefense + 1, updatedAt = NOW() WHERE id = #{userId}")
    int incrementTotalDefense(@Param("userId") Long userId);

    @Update("UPDATE farm_user SET friendCount = friendCount + #{count}, updatedAt = NOW() WHERE id = #{userId}")
    int updateFriendCount(@Param("userId") Long userId, @Param("count") Integer count);

    @Update("UPDATE farm_user SET visitedCount = visitedCount + 1, updatedAt = NOW() WHERE id = #{userId}")
    int incrementVisitedCount(@Param("userId") Long userId);

    @Update("UPDATE farm_user SET lastSignInDate = #{date}, consecutiveDays = consecutiveDays + 1, updatedAt = NOW() WHERE id = #{userId}")
    int updateSignIn(@Param("userId") Long userId, @Param("date") java.time.LocalDateTime date);

    @Update("UPDATE farm_user SET consecutiveDays = 1, lastSignInDate = #{date}, updatedAt = NOW() WHERE id = #{userId}")
    int resetSignIn(@Param("userId") Long userId, @Param("date") java.time.LocalDateTime date);
}
