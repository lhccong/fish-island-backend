package com.cong.fishisland.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户绑定邮箱请求
 *
 * @author Shing
 * date 10/4/2025
 */
@Data
public class UserBindEmailRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    private String email;

    private String code;

}
