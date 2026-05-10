package com.cong.fishisland.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 虎皮椒支付配置
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "xunhu.pay")
public class XunhuPayConfig {

    /**
     * 虎皮椒 APPID
     */
    private String appid;

    /**
     * 虎皮椒 APP 密钥
     */
    private String appsecret;

    /**
     * 支付网关地址（正式环境）
     */
    private String gatewayUrl = "https://api.xunhupay.com/payment/do.html";

    /**
     * 支付成功异步回调地址（notify_url），需要公网可访问
     */
    private String notifyUrl;

    /**
     * 支付成功同步跳转地址（return_url），可选
     */
    private String returnUrl;
}
