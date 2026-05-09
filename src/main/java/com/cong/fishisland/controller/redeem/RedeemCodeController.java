package com.cong.fishisland.controller.redeem;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.redeem.RedeemCodeAddRequest;
import com.cong.fishisland.model.dto.redeem.RedeemCodeQueryRequest;
import com.cong.fishisland.model.dto.redeem.RedeemCodeUseRequest;
import com.cong.fishisland.model.vo.redeem.RedeemCodeUseResultVO;
import com.cong.fishisland.model.vo.redeem.RedeemCodeVO;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.redeem.RedeemCodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 兑换码控制器
 *
 * @author cong
 */
@Slf4j
@RestController
@RequestMapping("/redeemCode")
@Api(tags = "兑换码接口")
public class RedeemCodeController {

    @Resource
    private RedeemCodeService redeemCodeService;

    @Resource
    private UserService userService;

    // ==================== 用户接口 ====================

    /**
     * 使用兑换码
     */
    @PostMapping("/use")
    @ApiOperation("使用兑换码")
    public BaseResponse<RedeemCodeUseResultVO> useRedeemCode(@RequestBody RedeemCodeUseRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        RedeemCodeUseResultVO result = redeemCodeService.useRedeemCode(request);
        return ResultUtils.success(result);
    }

    // ==================== 管理员接口 ====================

    /**
     * 创建兑换码（支持批量）
     */
    @PostMapping("/admin/add")
    @ApiOperation("创建兑换码（管理员）")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<List<String>> addRedeemCode(@RequestBody RedeemCodeAddRequest request) {
        ThrowUtils.throwIf(!userService.isAdmin(), ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        List<String> codes = redeemCodeService.addRedeemCode(request);
        return ResultUtils.success(codes);
    }

    /**
     * 分页查询兑换码列表（管理员）
     */
    @GetMapping("/admin/list")
    @ApiOperation("分页查询兑换码（管理员）")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<RedeemCodeVO>> listRedeemCodePage(RedeemCodeQueryRequest request) {
        ThrowUtils.throwIf(!userService.isAdmin(), ErrorCode.NO_AUTH_ERROR);
        if (request == null) {
            request = new RedeemCodeQueryRequest();
        }
        Page<RedeemCodeVO> page = redeemCodeService.listRedeemCodePage(request);
        return ResultUtils.success(page);
    }

    /**
     * 删除兑换码（管理员）
     */
    @DeleteMapping("/admin/delete/{id}")
    @ApiOperation("删除兑换码（管理员）")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteRedeemCode(@PathVariable Long id) {
        ThrowUtils.throwIf(!userService.isAdmin(), ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "ID非法");
        boolean result = redeemCodeService.deleteRedeemCode(id);
        return ResultUtils.success(result);
    }
}
