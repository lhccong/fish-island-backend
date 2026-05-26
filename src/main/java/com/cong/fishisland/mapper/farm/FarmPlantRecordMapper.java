package com.cong.fishisland.mapper.farm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.farm.FarmPlantRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface FarmPlantRecordMapper extends BaseMapper<FarmPlantRecord> {

    /**
     * 原子增加被偷积分，确保不超过可偷上限（baseReward - minReward）。
     */
    @Update("UPDATE farm_plant_record SET stolenPoints = stolenPoints + #{stealPoints}, "
            + "stolenCount = IFNULL(stolenCount, 0) + 1 "
            + "WHERE id = #{id} AND harvested = 0 "
            + "AND stolenPoints + #{stealPoints} <= #{baseReward} - #{minReward}")
    int incrementStolenPointsIfAllowed(@Param("id") Long id,
                                       @Param("stealPoints") int stealPoints,
                                       @Param("baseReward") int baseReward,
                                       @Param("minReward") int minReward);

    @Update("UPDATE farm_plant_record SET harvested = 1, harvestedTime = #{now} "
            + "WHERE id = #{id} AND harvested = 0")
    int markHarvestedIfNot(@Param("id") Long id, @Param("now") LocalDateTime now);
}
