package com.cong.fishisland.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.vo.IpLocationVO;
import com.cong.fishisland.service.IpLocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * IP 地理位置查询服务实现（封装 ip-api.com）
 */
@Service
@Slf4j
public class IpLocationServiceImpl implements IpLocationService {

    private static final String IP_API_BASE_URL = "http://ip-api.com/json";
    private static final String LANG = "zh-CN";
    private static final int TIMEOUT_MS = 5000;
    private static final String STATUS_SUCCESS = "success";

    @Override
    public IpLocationVO getLocationByIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法获取客户端 IP");
        }

        String url = IP_API_BASE_URL + "/" + ip.trim() + "?lang=" + LANG;
        try (HttpResponse response = HttpRequest.get(url).timeout(TIMEOUT_MS).execute()) {
            if (!response.isOk()) {
                log.warn("ip-api 请求失败, ip={}, status={}", ip, response.getStatus());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "IP 定位服务请求失败");
            }

            String body = response.body();
            IpLocationVO location = JSON.parseObject(body, IpLocationVO.class);
            if (location == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "IP 定位结果解析失败");
            }
            if (!STATUS_SUCCESS.equals(location.getStatus())) {
                String msg = StrUtil.isNotBlank(location.getMessage()) ? location.getMessage() : "IP 定位失败";
                throw new BusinessException(ErrorCode.OPERATION_ERROR, msg);
            }
            return location;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用 ip-api 异常, ip={}", ip, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "IP 定位服务异常");
        }
    }
}
