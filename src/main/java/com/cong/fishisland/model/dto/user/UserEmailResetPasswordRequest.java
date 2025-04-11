package com.cong.fishisland.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户邮箱重置密码请求
 *
 * @author Shing
 */
@Data
public class UserEmailResetPasswordRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    private String email;

    private String code;

    private String userPassword;

    private String checkPassword;


}
