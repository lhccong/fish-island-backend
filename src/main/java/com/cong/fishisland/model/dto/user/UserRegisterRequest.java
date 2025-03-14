package com.cong.fishisland.model.dto.user;

import java.io.Serializable;

import lombok.Data;

/**
 * 用户注册请求体
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    private String userAccount;

    private String email;

    private String code;

    private String userPassword;

    private String checkPassword;

    private String captchaVerification;
}