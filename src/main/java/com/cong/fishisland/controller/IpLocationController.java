package com.cong.fishisland.controller;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.vo.IpLocationVO;
import com.cong.fishisland.service.IpLocationService;
import com.cong.fishisland.utils.NetUtils;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * IP 地理位置接口
 */
@RestController
@RequestMapping("/ip")
@Slf4j
@RequiredArgsConstructor
public class IpLocationController {

    private final IpLocationService ipLocationService;

    /**
     * 根据当前请求客户端 IP 获取地理位置信息（无入参）
     */
    @GetMapping("/location")
    @ApiOperation(value = "获取当前用户 IP 地理位置")
    public BaseResponse<IpLocationVO> getLocation(HttpServletRequest request) {
        String clientIp = NetUtils.getIpAddress(request);
        return ResultUtils.success(ipLocationService.getLocationByIp(clientIp));
    }

}
