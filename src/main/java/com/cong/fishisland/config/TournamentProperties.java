package com.cong.fishisland.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 武道大会配置
 */
@Configuration
@ConfigurationProperties(prefix = "fishisland.tournament")
@Data
public class TournamentProperties {

    /**
     * 每日排行结算总积分池（按排名百分比分配）
     */
    private int rewardPoints = 100;
}
