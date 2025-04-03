package com.cong.fishisland.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI模型配置
 * @author Cong
 */
@Configuration
@ConfigurationProperties(prefix = "ai.model")
@Data
public class AIModelConfig {

    /**
     * 硅基流动模型KEY
     */
    private String siliconFlow;

    private  String Url;

    private  String Authorization;

}