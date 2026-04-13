package com.cong.fishisland.service.impl.fund;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.fund.IndexPositionMapper;
import com.cong.fishisland.model.entity.fund.IndexPosition;
import com.cong.fishisland.service.IndexPositionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 指数持仓服务实现
 * 
 * @author shing
 */
@Service
@Slf4j
public class IndexPositionServiceImpl extends ServiceImpl<IndexPositionMapper, IndexPosition>
        implements IndexPositionService {

    // ==================== 持仓查询 ====================

    @Override
    public IndexPosition getOrCreatePosition(Long userId, String indexCode) {
        LambdaQueryWrapper<IndexPosition> query = new LambdaQueryWrapper<>();
        query.eq(IndexPosition::getUserId, userId)
                .eq(IndexPosition::getIndexCode, indexCode);

        IndexPosition position = this.getOne(query);
        
        if (position == null) {
            position = createInitialPosition(userId, indexCode);
            this.save(position);
            log.info("创建持仓记录 - 用户: {}, 指数: {}", userId, indexCode);
        }
        
        return position;
    }

    @Override
    public BigDecimal getAvailableShares(Long userId, String indexCode) {
        IndexPosition position = getOrCreatePosition(userId, indexCode);
        return position.getAvailableShares();
    }

    // ==================== 买入相关 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addLockedShares(Long userId, String indexCode, BigDecimal shares, BigDecimal nav) {
        IndexPosition position = getOrCreatePosition(userId, indexCode);

        // 计算新的加权平均成本
        BigDecimal newAvgCost = calculateNewAvgCost(position, shares, nav);

        // 原子更新：totalShares 和 lockedShares 同时增加
        LambdaUpdateWrapper<IndexPosition> update = new LambdaUpdateWrapper<>();
        update.eq(IndexPosition::getUserId, userId)
                .eq(IndexPosition::getIndexCode, indexCode)
                .setSql("totalShares = totalShares + " + shares)
                .setSql("lockedShares = lockedShares + " + shares)
                .set(IndexPosition::getAvgCost, newAvgCost);

        boolean success = this.update(update);
        if (!success) {
            log.error("买入更新持仓失败 - 用户: {}, 指数: {}, 份额: {}", userId, indexCode, shares);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "买入更新持仓失败");
        }

        log.info("买入增加锁定份额 - 用户: {}, 指数: {}, 份额: {}, 新成本: {}", 
                userId, indexCode, shares, newAvgCost);
    }

    // ==================== 卖出相关 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reduceAvailableShares(Long userId, String indexCode, BigDecimal shares) {
        // 原子 CAS 更新，防止超卖
        LambdaUpdateWrapper<IndexPosition> update = new LambdaUpdateWrapper<>();
        update.eq(IndexPosition::getUserId, userId)
                .eq(IndexPosition::getIndexCode, indexCode)
                .ge(IndexPosition::getAvailableShares, shares) // CAS 条件：可用份额必须足够
                .setSql("totalShares = totalShares - " + shares)
                .setSql("availableShares = availableShares - " + shares);

        boolean success = this.update(update);
        
        if (success) {
            log.info("卖出扣减可用份额成功 - 用户: {}, 指数: {}, 份额: {}", userId, indexCode, shares);
        } else {
            log.warn("卖出失败：可用份额不足 - 用户: {}, 指数: {}, 需要: {}", userId, indexCode, shares);
        }
        
        return success;
    }

    // ==================== 份额解锁 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockAllLockedShares() {
        int count = this.baseMapper.unlockAllLockedShares();
        log.info("批量份额解锁完成 - 影响记录数: {}", count);
    }

    // ==================== 盈亏计算 ====================

    @Override
    public Long calculateProfitLoss(Long userId, String indexCode, BigDecimal currentNav) {
        IndexPosition position = getOrCreatePosition(userId, indexCode);
        
        if (position.getTotalShares().compareTo(BigDecimal.ZERO) == 0) {
            return 0L;
        }

        BigDecimal currentValue = position.getTotalShares().multiply(currentNav);
        BigDecimal costValue = position.getTotalShares().multiply(position.getAvgCost());
        BigDecimal profit = currentValue.subtract(costValue);

        return profit.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 创建初始持仓记录
     */
    private IndexPosition createInitialPosition(Long userId, String indexCode) {
        IndexPosition position = new IndexPosition();
        position.setUserId(userId);
        position.setIndexCode(indexCode);
        position.setTotalShares(BigDecimal.ZERO);
        position.setAvailableShares(BigDecimal.ZERO);
        position.setLockedShares(BigDecimal.ZERO);
        position.setAvgCost(BigDecimal.ZERO);
        return position;
    }

    /**
     * 计算新的加权平均成本
     * 
     * 公式：newAvgCost = (oldTotalValue + newValue) / newTotalShares
     * 
     * 边界情况处理：
     * - 首次买入（totalShares=0）：直接使用新买入的净值作为成本
     * - 后续买入：按加权平均计算
     */
    private BigDecimal calculateNewAvgCost(IndexPosition position, BigDecimal newShares, BigDecimal newNav) {
        BigDecimal oldTotalShares = position.getTotalShares();
        
        // 首次买入，直接使用新净值
        if (oldTotalShares.compareTo(BigDecimal.ZERO) == 0) {
            return newNav;
        }
        
        // 加权平均计算
        BigDecimal oldTotalValue = oldTotalShares.multiply(position.getAvgCost());
        BigDecimal newValue = newShares.multiply(newNav);
        BigDecimal newTotalShares = oldTotalShares.add(newShares);
        
        return oldTotalValue.add(newValue)
                .divide(newTotalShares, 6, RoundingMode.HALF_UP);
    }
}
