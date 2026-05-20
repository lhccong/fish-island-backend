package com.cong.fishisland.mapper.farm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.farm.FarmFriend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface FarmFriendMapper extends BaseMapper<FarmFriend> {

    @Update("UPDATE farm_friend SET lastVisitTime = #{lastVisitTime}, updatedAt = NOW() WHERE userId = #{userId} AND friendId = #{friendId}")
    int updateLastVisitTime(@Param("userId") Long userId, @Param("friendId") Long friendId, @Param("lastVisitTime") LocalDateTime lastVisitTime);

    @Update("UPDATE farm_friend SET stealCooldown = #{cooldownTime}, updatedAt = NOW() WHERE userId = #{userId} AND friendId = #{friendId}")
    int updateStealCooldown(@Param("userId") Long userId, @Param("friendId") Long friendId, @Param("cooldownTime") LocalDateTime cooldownTime);
}
