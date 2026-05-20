package com.cong.fishisland.service.impl.farm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.farm.FarmRankingMapper;
import com.cong.fishisland.model.dto.farm.RankingDTO;
import com.cong.fishisland.model.entity.farm.FarmRanking;
import com.cong.fishisland.model.enums.farm.FarmRankingTypeEnum;
import com.cong.fishisland.service.FarmRankingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FarmRankingServiceImpl extends ServiceImpl<FarmRankingMapper, FarmRanking> implements FarmRankingService {

    @Autowired
    private FarmRankingMapper rankingMapper;

    @Override
    public List<RankingDTO> getTodayStealExpRanking() {
        LocalDate today = LocalDate.now();
        return rankingMapper.selectTodayStealExpRanking(today, FarmRankingTypeEnum.STEAL_EXP.getValue());
    }

    @Override
    public List<RankingDTO> getTodayStealCountRanking() {
        LocalDate today = LocalDate.now();
        return rankingMapper.selectTodayStealCountRanking(today, FarmRankingTypeEnum.STEAL_COUNT.getValue());
    }

    @Override
    public List<RankingDTO> getTodayDefenseRanking() {
        LocalDate today = LocalDate.now();
        return rankingMapper.selectTodayDefenseRanking(today, FarmRankingTypeEnum.DEFENSE.getValue());
    }

    @Override
    public List<RankingDTO> getTotalStealExpRanking() {
        return rankingMapper.selectTotalStealExpRanking(FarmRankingTypeEnum.STEAL_EXP.getValue());
    }

    @Override
    public List<RankingDTO> getTotalStealCountRanking() {
        return rankingMapper.selectTotalStealCountRanking(FarmRankingTypeEnum.STEAL_COUNT.getValue());
    }

    @Override
    public List<RankingDTO> getTotalDefenseRanking() {
        return rankingMapper.selectTotalDefenseRanking(FarmRankingTypeEnum.DEFENSE.getValue());
    }

    @Override
    public void updateStealCountRanking(Long stealerId) {
        updateRanking(stealerId, FarmRankingTypeEnum.STEAL_COUNT, 1);
    }

    @Override
    public void updateDefenseRanking(Long ownerId, int damage) {
        updateRanking(ownerId, FarmRankingTypeEnum.DEFENSE, damage);
    }

    private void updateRanking(Long userId, FarmRankingTypeEnum type, int value) {
        LocalDate today = LocalDate.now();
        int updated = rankingMapper.updateRankingValue(userId, type.getValue(), today, value, value);
        if (updated == 0) {
            LocalDateTime now = LocalDateTime.now();
            FarmRanking ranking = new FarmRanking();
            ranking.setUserId(userId);
            ranking.setType(type.getValue());
            ranking.setDate(today);
            ranking.setTodayValue(value);
            ranking.setTotalValue(value);
            ranking.setCreateTime(now);
            ranking.setUpdateTime(now);
            save(ranking);
        }
    }
}
