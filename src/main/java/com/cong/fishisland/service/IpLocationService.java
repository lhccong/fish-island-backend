package com.cong.fishisland.service;

import com.cong.fishisland.model.vo.IpLocationVO;

/**
 * IP 地理位置查询服务
 */
public interface IpLocationService {

    /**
     * 根据 IP 地址查询地理位置信息
     *
     * @param ip 客户端 IP
     * @return 地理位置信息
     */
    IpLocationVO getLocationByIp(String ip);
}
