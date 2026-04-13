package com.cong.fishisland.controller.fund;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.fund.IndexBuyRequest;
import com.cong.fishisland.model.dto.fund.IndexSellRequest;
import com.cong.fishisland.model.dto.fund.IndexTransactionQueryRequest;
import com.cong.fishisland.model.vo.fund.IndexPositionVO;
import com.cong.fishisland.model.vo.fund.IndexTradeResultVO;
import com.cong.fishisland.model.vo.fund.IndexTransactionVO;
import com.cong.fishisland.service.IndexTradeService;
import com.cong.fishisland.service.UserService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 指数交易接口
 *
 * @author shing
 */
@RestController
@RequestMapping("/index/trade")
@Slf4j
public class IndexTradeController {

    @Resource
    private IndexTradeService indexTradeService;

    @Resource
    private UserService userService;

    private static final String INDEX_CODE = "sh000001";

    /**
     * 买入指数
     */
    @PostMapping("/buy")
    @ApiOperation(value = "买入指数")
    @SaCheckLogin
    public BaseResponse<IndexTradeResultVO> buyIndex(@RequestBody IndexBuyRequest request) {
        // 默认指数代码
        if (request.getIndexCode() == null || request.getIndexCode().trim().isEmpty()) {
            request.setIndexCode(INDEX_CODE);
        }
        
        Long userId = userService.getLoginUser().getId();
        IndexTradeResultVO result = indexTradeService.buyIndexWithResult(
                userId, 
                request.getIndexCode(), 
                request.getAmount()
        );
        return ResultUtils.success(result);
    }

    /**
     * 卖出指数
     */
    @PostMapping("/sell")
    @ApiOperation(value = "卖出指数")
    @SaCheckLogin
    public BaseResponse<IndexTradeResultVO> sellIndex(@RequestBody IndexSellRequest request) {
        // 默认指数代码
        if (request.getIndexCode() == null || request.getIndexCode().trim().isEmpty()) {
            request.setIndexCode(INDEX_CODE);
        }
        
        Long userId = userService.getLoginUser().getId();
        IndexTradeResultVO result = indexTradeService.sellIndexWithResult(
                userId, 
                request.getIndexCode(), 
                request.getShares()
        );
        return ResultUtils.success(result);
    }

    /**
     * 获取用户持仓信息
     */
    @GetMapping("/position")
    @ApiOperation(value = "获取用户持仓信息")
    @SaCheckLogin
    public BaseResponse<IndexPositionVO> getPosition() {
        Long userId = userService.getLoginUser().getId();
        IndexPositionVO position = indexTradeService.getUserPosition(userId, INDEX_CODE);
        return ResultUtils.success(position);
    }

    /**
     * 获取交易记录列表
     */
    @PostMapping("/transactions")
    @ApiOperation(value = "获取交易记录列表")
    @SaCheckLogin
    public BaseResponse<Page<IndexTransactionVO>> getTransactions(@RequestBody IndexTransactionQueryRequest queryRequest) {
        Long userId = userService.getLoginUser().getId();

        // 默认指数代码
        if (queryRequest.getIndexCode() == null || queryRequest.getIndexCode().trim().isEmpty()) {
            queryRequest.setIndexCode(INDEX_CODE);
        }

        // 参数校验
        int current = Math.max(queryRequest.getCurrent(), 1);
        int pageSize = Math.min(Math.max(queryRequest.getPageSize(), 1), 100); // 限制最大100条

        Page<IndexTransactionVO> page = indexTradeService.getUserTransactionPage(
                userId,
                queryRequest.getIndexCode(),
                (long) current,
                (long) pageSize
        );

        return ResultUtils.success(page);
    }

    /**
     * 获取待结算交易
     */
    @GetMapping("/pending")
    @ApiOperation(value = "获取待结算交易")
    @SaCheckLogin
    public BaseResponse<List<IndexTransactionVO>> getPendingTransactions() {
        Long userId = userService.getLoginUser().getId();
        List<IndexTransactionVO> list = indexTradeService.getUserPendingTransactions(userId, INDEX_CODE);
        return ResultUtils.success(list);
    }
}
