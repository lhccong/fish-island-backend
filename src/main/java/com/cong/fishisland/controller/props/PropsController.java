package com.cong.fishisland.controller.props;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.model.dto.props.PropsPurchaseRequest;
import com.cong.fishisland.model.dto.props.PropsQueryRequest;
import com.cong.fishisland.model.entity.props.Props;
import com.cong.fishisland.model.vo.props.PropsVO;
import com.cong.fishisland.service.PropsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 道具接口
 *
 * @author cong
 */
@RestController
@RequestMapping("/props")
@Slf4j
@RequiredArgsConstructor
//@Api(tags = "道具接口")
public class PropsController {

    private final PropsService propsService;

    /**
     * 分页获取道具列表
     *
     * @param propsQueryRequest 道具查询请求
     * @return {@link BaseResponse}<{@link Page}<{@link PropsVO}>>
     */
    @GetMapping("/list/page")
    @ApiOperation("分页获取道具列表")
    public BaseResponse<Page<PropsVO>> listPropsPage(PropsQueryRequest propsQueryRequest) {
        long current = propsQueryRequest.getCurrent();
        long size = propsQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Props> propsPage = propsService.page(new Page<>(current, size),
                propsService.getQueryWrapper(propsQueryRequest));
        return ResultUtils.success(propsService.getPropsVOPage(propsPage));
    }

    /**
     * 购买道具
     *
     * @param propsPurchaseRequest 道具购买请求
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @PostMapping("/purchase")
    @ApiOperation("购买道具")
    @SaCheckLogin
    public BaseResponse<Boolean> purchaseProps(@RequestBody PropsPurchaseRequest propsPurchaseRequest) {
        return ResultUtils.success(propsService.purchaseProps(propsPurchaseRequest));
    }
} 