package com.cong.fishisland.mapper.farm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.farm.FarmLand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface FarmLandMapper extends BaseMapper<FarmLand> {

    @Update("UPDATE farm_land SET harvestTime = #{harvestTime}, lastWateredTime = #{now}, updateTime = #{now} "
            + "WHERE id = #{land.id} AND userId = #{land.userId} AND locked = 0 AND status = 1 "
            + "AND plantedCropId = #{land.plantedCropId} AND plantedTime = #{land.plantedTime} "
            + "AND harvestTime = #{land.harvestTime} AND harvestTime > #{now} "
            + "AND (lastWateredTime IS NULL OR lastWateredTime <= #{cooldownBefore})")
    int waterIfAllowed(@Param("land") FarmLand land,
                       @Param("harvestTime") LocalDateTime harvestTime,
                       @Param("now") LocalDateTime now,
                       @Param("cooldownBefore") LocalDateTime cooldownBefore);

    @Update("UPDATE farm_land SET status = #{matureStatus}, updateTime = #{now} "
            + "WHERE status = #{plantingStatus} AND harvestTime <= #{now}")
    int updateMatureLands(@Param("now") LocalDateTime now,
                          @Param("plantingStatus") int plantingStatus,
                          @Param("matureStatus") int matureStatus);
}
