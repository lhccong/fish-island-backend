package com.cong.fishisland.mapper.pet;

import com.cong.fishisland.model.entity.pet.PetTreasureDig;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;

public interface PetTreasureMapper {
    String COLUMNS = "id, user_id AS userId, dig_day AS digDay, slot, site_id AS siteId, "
            + "ready_at AS readyAt, reward, completed_at AS completedAt";

    @Select("SELECT " + COLUMNS + " FROM pet_treasure_dig WHERE user_id=#{userId} ORDER BY ready_at DESC LIMIT 1")
    PetTreasureDig latest(@Param("userId") Long userId);

    @Select("SELECT " + COLUMNS + " FROM pet_treasure_dig WHERE user_id=#{userId} AND id=#{id}")
    PetTreasureDig owned(@Param("userId") Long userId, @Param("id") String id);

    @Select("SELECT COUNT(*) FROM pet_treasure_dig WHERE user_id=#{userId} AND dig_day=#{day}")
    int countDay(@Param("userId") Long userId, @Param("day") LocalDate day);

    @Select("SELECT COUNT(*) FROM fish_pet WHERE userId=#{userId} AND isDelete=0")
    int companionCount(@Param("userId") Long userId);

    @Insert("INSERT INTO pet_treasure_dig (id,user_id,dig_day,slot,site_id,ready_at,reward) "
            + "VALUES (#{id},#{userId},#{digDay},#{slot},#{siteId},#{readyAt},#{reward})")
    int insert(PetTreasureDig dig);

    @Update("UPDATE pet_treasure_dig SET completed_at=#{now} WHERE id=#{id} AND user_id=#{userId} AND completed_at IS NULL")
    int complete(@Param("userId") Long userId, @Param("id") String id, @Param("now") long now);

}
