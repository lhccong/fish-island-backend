package com.cong.fishisland.model.entity.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 第三方应用（OAuth2 客户端）
 *
 * @author cong
 */
@Data
@TableName("fish_auth")
public class FishAuth implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 应用名称 */
    private String appName;

    /** 应用网站地址 */
    private String appWebsite;

    /** 应用描述 */
    private String appDesc;

    /** 回调地址（多个用逗号分隔） */
    private String redirectUri;

    /** Client ID */
    private String clientId;

    /** Client Secret（加密存储） */
    private String clientSecret;

    /** 创建者用户 ID */
    private Long userId;

    /** 状态：0-禁用，1-启用 */
    private Integer status;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}
