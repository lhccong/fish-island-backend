package com.cong.fishisland.model.vo.user;

import cn.dev33.satoken.stp.SaTokenInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 使用Token登录
 * @author cong
 * @date 2023/09/11
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TokenLoginUserVo extends LoginUserVO implements Serializable {

    private static final long serialVersionUID = 2405172041950251807L;

    /**
     *token的信息
     */
    private transient SaTokenInfo saTokenInfo;

}
