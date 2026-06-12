package com.cong.fishisland.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * NudeNet 图片内容检测配置
 */
@Configuration
@ConfigurationProperties(prefix = "fishisland.nudenet")
@Data
public class NudeNetProperties {

    /**
     * 是否启用检测
     */
    private boolean enabled = false;

    /**
     * NudeNet infer 接口地址
     */
    private String inferUrl = "http://localhost:8080/infer";

    /**
     * 命中阈值，score >= threshold 视为违规
     */
    private double scoreThreshold = 0.6;

    /**
     * 检测服务不可用时是否放行上传
     */
    private boolean failOpen = true;

    /**
     * 超时时间（秒）
     */
    private int timeoutSeconds = 15;

    /**
     * 违规图片通知接收人用户 ID
     */
    private long notifyUserId = 1L;

    /**
     * 需要拦截的检测类别
     */
    private List<String> blockedClasses = Arrays.asList(
            "FEMALE_BREAST_EXPOSED",
            "FEMALE_GENITALIA_EXPOSED",
            "MALE_GENITALIA_EXPOSED",
            "MALE_BREAST_EXPOSED",
            "BUTTOCKS_EXPOSED",
            "ANUS_EXPOSED",
            "BELLY_EXPOSED"
    );

    public Set<String> blockedClassSet() {
        return new HashSet<>(blockedClasses);
    }
}
