package com.cong.fishisland.service.impl.farm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.farm.FarmRankingMapper;
import com.cong.fishisland.model.dto.farm.RankingDTO;
import com.cong.fishisland.model.entity.farm.FarmRanking;
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
        return rankingMapper.selectTodayStealExpRanking(today);
    }

    @Override
    public List<RankingDTO> getTodayStealCountRanking() {
        LocalDate today = LocalDate.now();
        return rankingMapper.selectTodayStealCountRanking(today);
    }

    @Override
    public List<RankingDTO> getTodayDefenseRanking() {
        LocalDate today = LocalDate.now();
        return rankingMapper.selectTodayDefenseRanking(today);
    }

    @Override
    public List<RankingDTO> getTotalStealExpRanking() {
        return rankingMapper.selectTotalStealExpRanking();
    }

    @Override
    public List<RankingDTO> getTotalStealCountRanking() {
        return rankingMapper.selectTotalStealCountRanking();
    }

    @Override
    public List<RankingDTO> getTotalDefenseRanking() {
        return rankingMapper.selectTotalDefenseRanking();
    }

    @Override
    public void updateStealRanking(Long stealerId, int expGained) {
        updateRanking(stealerId, "steal_exp", expGained);
        updateRanking(stealerId, "steal_count", 1);
    }

    @Override
    public void updateDefenseRanking(Long ownerId, int damage) {
        updateRanking(ownerId, "defense", damage);
    }

    private void updateRanking(Long userId, String type, int value) {
        LocalDate today = LocalDate.now();
        int updated = rankingMapper.updateRankingValue(userId, type, today, value, value);
        if (updated == 0) {
            LocalDateTime now = LocalDateTime.now();
            FarmRanking ranking = new FarmRanking();
            ranking.setUserId(userId);
            ranking.setType(type);
            ranking.setDate(today);
            ranking.setTodayValue(value);
            ranking.setTotalValue(value);
            ranking.setCreatedAt(now);
            ranking.setUpdatedAt(now);
            save(ranking);
        }
    }
}
