package com.cong.fishisland.controller.fund;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.fund.AddFundRequest;
import com.cong.fishisland.model.dto.fund.DeleteFundRequest;
import com.cong.fishisland.model.dto.fund.EditFundRequest;
import com.cong.fishisland.model.dto.fund.UpdateFundRequest;
import com.cong.fishisland.model.vo.fund.FundListVO;
import com.cong.fishisland.model.vo.fund.MarketIndexVO;
import com.cong.fishisland.service.FundService;
import com.cong.fishisland.service.UserService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 基金持仓接口
 *
 * @author shing
 */
@RestController
@RequestMapping("/fund")
@Slf4j
public class FundController {

    @Resource
    private FundService fundService;

    @Resource
    private UserService userService;

    /**
     * 添加基金
     *
     * @param addFundRequest 添加基金请求
     * @return 是否成功
     */
    @PostMapping("/add")
    @ApiOperation(value = "添加基金")
    @SaCheckLogin
    public BaseResponse<Boolean> addFund(@RequestBody AddFundRequest addFundRequest) {
        Long userId = userService.getLoginUser().getId();
        Boolean result = fundService.addFund(addFundRequest, userId);
        return ResultUtils.success(result);
    }

    /**
     * 删除基金
     *
     * @param deleteFundRequest 删除基金请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    @ApiOperation(value = "删除基金")
    @SaCheckLogin
    public BaseResponse<Boolean> deleteFund(@RequestBody DeleteFundRequest deleteFundRequest) {
        Long userId = userService.getLoginUser().getId();
        Boolean result = fundService.deleteFund(deleteFundRequest, userId);
        return ResultUtils.success(result);
    }

    /**
     * 编辑基金
     *
     * @param editFundRequest 编辑基金请求
     * @return 是否成功
     */
    @PostMapping("/edit")
    @ApiOperation(value = "编辑基金")
    @SaCheckLogin
    public BaseResponse<Boolean> editFund(@RequestBody EditFundRequest editFundRequest) {
        Long userId = userService.getLoginUser().getId();
        Boolean result = fundService.editFund(editFundRequest, userId);
        return ResultUtils.success(result);
    }

    /**
     * 管理员更新基金（直接修改份额和成本）
     *
     * @param updateFundRequest 更新基金请求
     * @return 是否成功
     */
    @PostMapping("/update")
    @ApiOperation(value = "管理员更新基金")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateFund(@RequestBody UpdateFundRequest updateFundRequest) {
        Boolean result = fundService.updateFund(updateFundRequest);
        return ResultUtils.success(result);
    }

    /**
     * 获取基金持仓列表（包含实时数据）
     *
     * @return 基金持仓列表
     */
    @GetMapping("/list")
    @ApiOperation(value = "获取基金持仓列表")
    @SaCheckLogin
    public BaseResponse<FundListVO> getFundList() {
        Long userId = userService.getLoginUser().getId();
        FundListVO fundList = fundService.getFundList(userId);
        return ResultUtils.success(fundList);
    }

    /**
     * 获取国内主要指数行情
     *
     * @return 指数行情列表
     */
    @GetMapping("/indices")
    @ApiOperation(value = "获取国内主要指数行情")
    @SaCheckLogin
    public BaseResponse<List<MarketIndexVO>> getMajorIndices() {
        List<MarketIndexVO> indices = fundService.getMajorIndices();
        return ResultUtils.success(indices);
    }
}


