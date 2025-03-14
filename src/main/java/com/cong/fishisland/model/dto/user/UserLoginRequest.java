package com.cong.fishisland.model.dto.user;

import java.io.Serializable;
import lombok.Data;

/**
 * 用户登录请求
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    private String userAccount;

    private String email;

    private String userPassword;
}
