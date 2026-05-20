package com.cong.fishisland.mapper.farm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.farm.FarmLand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface FarmLandMapper extends BaseMapper<FarmLand> {

    @Update("UPDATE farm_land SET status = 2, updatedAt = #{now} WHERE status = 1 AND harvestTime <= #{now}")
    int updateMatureLands(@Param("now") LocalDateTime now);
}
