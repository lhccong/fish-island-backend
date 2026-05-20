package com.cong.fishisland.mapper.farm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.dto.farm.RankingDTO;
import com.cong.fishisland.model.entity.farm.FarmRanking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface FarmRankingMapper extends BaseMapper<FarmRanking> {

    @Update("UPDATE farm_ranking SET todayValue = todayValue + #{todayValue}, totalValue = totalValue + #{totalValue}, updatedAt = NOW() " +
            "WHERE userId = #{userId} AND type = #{type} AND date = #{date}")
    int updateRankingValue(@Param("userId") Long userId, @Param("type") String type, @Param("date") LocalDate date,
                           @Param("todayValue") Integer todayValue, @Param("totalValue") Integer totalValue);

    List<RankingDTO> selectTodayStealExpRanking(@Param("date") LocalDate date);

    List<RankingDTO> selectTodayStealCountRanking(@Param("date") LocalDate date);

    List<RankingDTO> selectTodayDefenseRanking(@Param("date") LocalDate date);

    List<RankingDTO> selectTotalStealExpRanking();

    List<RankingDTO> selectTotalStealCountRanking();

    List<RankingDTO> selectTotalDefenseRanking();
}
