package com.cong.fishisland.mapper;

import lombok.Data;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AchievementMapper {
    Long lockPoints(@Param("userId") long userId);

    Integer userLevel(@Param("userId") long userId);

    Integer farmLevel(@Param("userId") long userId);

    Long openedPacks(@Param("userId") long userId);

    Integer petLevel(@Param("userId") long userId);

    int turntableDrawCount(@Param("userId") long userId);

    List<Equipment> equipmentLevels(@Param("userId") long userId);

    List<Reward> rewards(@Param("userId") long userId);

    void saveReward(@Param("userId") long userId, @Param("achievementId") String achievementId,
                    @Param("subjectId") int subjectId, @Param("claimedTiers") int claimedTiers);

    @Data
    class Equipment {
        private int slot;
        private int level;
    }

    @Data
    class Reward {
        private String achievementId;
        private int subjectId;
        private int claimedTiers;
    }
}
