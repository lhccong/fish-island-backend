package com.cong.fishisland.model.vo.user;

import lombok.Data;

/**
 * 三方平台绑定
 *
 * @author lijing
 * @date 2025-04-11
 */
@Data
public class PlatformBindVO {

    /**
     * 平台 github/gitee
     */
    private String platform;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

}
