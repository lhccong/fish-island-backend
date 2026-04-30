package com.cong.fishisland.model.entity.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * OAuth2 授权码
 *
 * @author cong
 */
@Data
@TableName("fish_auth_code")
public class FishAuthCode implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 授权码 */
    private String code;

    /** Client ID */
    private String clientId;

    /** 授权用户 ID */
    private Long userId;

    /** 回调地址 */
    private String redirectUri;

    /** 授权范围 */
    private String scope;

    /** 是否已使用：0-未使用，1-已使用 */
    private Integer used;

    /** 过期时间 */
    private Date expireTime;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
