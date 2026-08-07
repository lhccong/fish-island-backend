package com.cong.fishisland.game.landlords.service;

import lombok.Getter;

/**
 * 游戏业务异常
 * 用于处理游戏中的业务逻辑错误
 *
 * @author cong
 */
@Getter
public class GameBusinessException extends RuntimeException {

    private final String errorCode;

    public GameBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public GameBusinessException(String message) {
        super(message);
        this.errorCode = "ERROR";
    }

    public GameBusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ERROR";
    }
}
