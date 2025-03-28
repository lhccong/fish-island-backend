package com.github.yuyuanweb.mianshiyaplugin.model.response;

import lombok.Data;

import java.util.Date;

/**
 * @author pine
 */
@Data
public class User {

    private Long id;

    private String userName;

    private String userRole;

    private Date vipExpireTime;

}
