package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.fund.IndexTradeRecord;
import com.cong.fishisland.model.vo.fund.IndexPositionVO;
import com.cong.fishisland.model.vo.fund.IndexTradeResultVO;
import com.cong.fishisland.model.vo.fund.IndexTransactionVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 指数交易服务
 * 
 * 职责：处理指数买卖交易、持仓查询、交易记录查询等业务逻辑
 * 
 * @author shing
 */
public interface IndexTradeService extends IService<IndexTradeRecord> {

    // ==================== 交易操作（对外接口） ====================

    /**
     * 买入指数（完整业务流程）
     * 
     * 流程：
     * 1. 校验交易时间
     * 2. 扣除用户积分
     * 3. 增加锁定份额（T+1 可卖）
     * 4. 记录交易流水
     * 
     * @param userId 用户ID
     * @param indexCode 指数代码
     * @param amount 投入积分
     * @return 交易结果VO
     */
    IndexTradeResultVO buyIndexWithResult(Long userId, String indexCode, Long amount);

    /**
     * 卖出指数（完整业务流程）
     * 
     * 流程：
     * 1. 校验交易时间
     * 2. 扣减可用份额（原子操作）
     * 3. 实时返还积分（T+0）
     * 4. 记录交易流水
     * 
     * @param userId 用户ID
     * @param indexCode 指数代码
     * @param shares 卖出份额
     * @return 交易结果VO
     */
    IndexTradeResultVO sellIndexWithResult(Long userId, String indexCode, BigDecimal shares);

    // ==================== 持仓查询 ====================

    /**
     * 获取用户持仓信息（包含实时净值、盈亏等）
     * 
     * @param userId 用户ID
     * @param indexCode 指数代码
     * @return 持仓信息VO
     */
    IndexPositionVO getUserPosition(Long userId, String indexCode);

    // ==================== 交易记录查询 ====================

    /**
     * 分页查询用户交易记录
     * 
     * @param userId 用户ID
     * @param indexCode 指数代码
     * @param current 当前页
     * @param pageSize 每页大小
     * @return 交易记录分页
     */
    Page<IndexTransactionVO> getUserTransactionPage(Long userId, String indexCode, Long current, Long pageSize);

    /**
     * 查询用户待结算交易列表（已废弃，T+0 模式下无待结算）
     * 
     * @param userId 用户ID
     * @param indexCode 指数代码
     * @return 待结算交易列表
     */
    @Deprecated
    List<IndexTransactionVO> getUserPendingTransactions(Long userId, String indexCode);

    /**
     * 查询用户交易记录（原始实体）
     * 
     * @param userId 用户ID
     * @param indexCode 指数代码
     * @return 交易记录列表
     */
    List<IndexTradeRecord> getUserTransactions(Long userId, String indexCode);
}
