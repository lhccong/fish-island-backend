package com.cong.fishisland.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 全局未捕获异常处理程序
 *
 * @author cong
 * @date 2023/10/31
 */
@Slf4j
public class GlobalUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    /**
     * 实例
     */
    private static final GlobalUncaughtExceptionHandler INSTANCE = new GlobalUncaughtExceptionHandler();

    private GlobalUncaughtExceptionHandler() {
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error("Exception in thread {} ", t.getName(), e);
    }

    public static GlobalUncaughtExceptionHandler getInstance() {
        return INSTANCE;
    }

}
