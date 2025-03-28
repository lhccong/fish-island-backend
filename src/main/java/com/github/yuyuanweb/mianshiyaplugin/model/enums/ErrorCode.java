package com.github.yuyuanweb.mianshiyaplugin.model.enums;

/**
 * 错误码
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public enum ErrorCode {

    SUCCESS(0, "ok"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_VIP_AUTH_ERROR(40102, "无会员权限"),
    ;

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
