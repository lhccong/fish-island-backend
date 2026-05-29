package com.cong.fishisland.controller.fund;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.fund.IndexBuyRequest;
import com.cong.fishisland.model.dto.fund.IndexSellRequest;
import com.cong.fishisland.model.dto.fund.IndexTransactionQueryRequest;
import com.cong.fishisland.model.enums.fund.FundConstants;
import com.cong.fishisland.model.vo.fund.IndexPositionVO;
import com.cong.fishisland.model.vo.fund.IndexTradeResultVO;
import com.cong.fishisland.model.vo.fund.IndexTransactionVO;
import com.cong.fishisland.service.IndexTradeService;
import com.cong.fishisland.service.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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

    /**
     * 买入指数
     */
    @PostMapping("/buy")
    @ApiOperation(value = "买入指数", notes = "支持 sh000001(上证)、sz399001(深证成指)、sz399006(创业板指)、sh000300(沪深300)、sh000016(上证50)")
    @SaCheckLogin
    public BaseResponse<IndexTradeResultVO> buyIndex(@RequestBody IndexBuyRequest request) {
        request.setIndexCode(FundConstants.normalizeIndexCode(request.getIndexCode()));

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
    @ApiOperation(value = "卖出指数", notes = "支持 sh000001(上证)、sz399001(深证成指)、sz399006(创业板指)、sh000300(沪深300)、sh000016(上证50)")
    @SaCheckLogin
    public BaseResponse<IndexTradeResultVO> sellIndex(@RequestBody IndexSellRequest request) {
        request.setIndexCode(FundConstants.normalizeIndexCode(request.getIndexCode()));

        Long userId = userService.getLoginUser().getId();
        IndexTradeResultVO result = indexTradeService.sellIndexWithResult(
                userId,
                request.getIndexCode(),
                request.getShares()
        );
        return ResultUtils.success(result);
    }

    /**
     * 获取用户单个指数持仓信息
     */
    @GetMapping("/position")
    @ApiOperation(value = "获取用户持仓信息")
    @SaCheckLogin
    public BaseResponse<IndexPositionVO> getPosition(
            @ApiParam(value = "指数代码，默认 sh000001", example = "sh000001")
            @RequestParam(required = false) String indexCode) {
        Long userId = userService.getLoginUser().getId();
        IndexPositionVO position = indexTradeService.getUserPosition(
                userId,
                FundConstants.normalizeIndexCode(indexCode)
        );
        return ResultUtils.success(position);
    }

    /**
     * 获取用户全部支持指数的持仓列表
     */
    @GetMapping("/positions")
    @ApiOperation(value = "获取用户全部指数持仓")
    @SaCheckLogin
    public BaseResponse<List<IndexPositionVO>> getPositions() {
        Long userId = userService.getLoginUser().getId();
        List<IndexPositionVO> positions = indexTradeService.getUserPositions(userId);
        return ResultUtils.success(positions);
    }

    /**
     * 获取交易记录列表
     */
    @PostMapping("/transactions")
    @ApiOperation(value = "获取交易记录列表", notes = "无需传 indexCode，默认返回当前用户全部指数的交易记录；可按 tradeType、status 筛选")
    @SaCheckLogin
    public BaseResponse<Page<IndexTransactionVO>> getTransactions(@RequestBody IndexTransactionQueryRequest queryRequest) {
        Long userId = userService.getLoginUser().getId();

        String indexCode = resolveOptionalIndexCode(queryRequest.getIndexCode());

        int current = Math.max(queryRequest.getCurrent(), 1);
        int pageSize = Math.min(Math.max(queryRequest.getPageSize(), 1), 100);

        Page<IndexTransactionVO> page = indexTradeService.getUserTransactionPage(
                userId,
                indexCode,
                queryRequest.getTradeType(),
                queryRequest.getStatus(),
                (long) current,
                (long) pageSize
        );

        return ResultUtils.success(page);
    }

    /**
     * 可选指数过滤：未传 indexCode 时不按指数筛选
     */
    private String resolveOptionalIndexCode(String rawIndexCode) {
        if (rawIndexCode == null || rawIndexCode.trim().isEmpty()) {
            return null;
        }
        String normalized = FundConstants.normalizeIndexCode(rawIndexCode.trim());
        if (!FundConstants.isSupportedIndex(normalized)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "暂不支持该指数，当前支持：上证指数(sh000001)、深证成指(sz399001)、创业板指(sz399006)、沪深300(sh000300)、上证50(sh000016)");
        }
        return normalized;
    }

}
