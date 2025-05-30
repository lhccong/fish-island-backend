package com.cong.fishisland.aop;

import com.cong.fishisland.annotation.NoRepeatSubmit;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.RedisKey;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

/**
 * 防重复提交切面
 *
 * @author 许林涛
 * @date 2025年05月30日 9:08
 */
@Aspect
@Component
@Slf4j
public class NoRepeatSubmitAspect {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private HttpServletRequest httpServletRequest;

    /**
     * 定义一个常量，用于存储Lua脚本
     */
    private static final String LUA_SCRIPT =
            "if redis.call('exists', KEYS[1]) == 0 then " +
                    "redis.call('set', KEYS[1], '1', 'EX', ARGV[1]); " +
                    "return 1; " +
                    "else " +
                    "return 0; " +
                    "end";

    /**
     * 定义一个常量，用于存储DefaultRedisScript的实例
     */
    private static final DefaultRedisScript<Long> REDIS_SCRIPT =
            new DefaultRedisScript<>(LUA_SCRIPT, Long.class);

    /**
     * 环绕通知方法，用于处理重复提交的请求
     * 该方法会在操作执行前后进行拦截，主要用于防止重复提交
     *
     * @param pjp            连接点对象，用于操作原方法的执行
     * @param noRepeatSubmit 无重复提交注解对象，用于获取配置信息
     * @return 原方法的执行结果
     * @throws Throwable 如果方法执行过程中发生异常，将会抛出Throwable异常
     *                   <p>
     *                   为什么使用环绕通知：
     *                   环绕通知可以在方法执行前后插入自定义的逻辑，这里用于检查和防止重复提交
     *                   通过在方法执行前检查Redis中是否存在特定的key，我们可以有效阻止重复提交
     *                   如果存在该key，表示该请求已经提交过，此时抛出异常，阻止方法的执行
     *                   如果不存在，我们将创建该key，并设置过期时间，以确保请求在指定时间内不会被重复提交
     */
    @Around("@annotation(noRepeatSubmit)")
    public Object around(ProceedingJoinPoint pjp, NoRepeatSubmit noRepeatSubmit) throws Throwable {
        // 获取用于防止重复提交的唯一key
        String key = getKey();
        // 获取配置的过期时间
        long expireTime = noRepeatSubmit.expire();
        // 验证expireTime是否有效
        ThrowUtils.throwIf(expireTime <= 0, ErrorCode.PARAMS_ERROR, "过期时间不合法");
        // 调用Redis的Lua脚本，检查是否存在该key，如果存在则返回0，表示重复提交
        Long result = stringRedisTemplate.execute(REDIS_SCRIPT, Collections.singletonList(key), String.valueOf(expireTime));
        ThrowUtils.throwIf(result == null, ErrorCode.SYSTEM_ERROR, "Redis服务异常");
        // 如果key已经存在，表示这是一个重复的提交请求
        ThrowUtils.throwIf(result == 0L, ErrorCode.REPEAT_SUBMIT_ERROR);
        try {
            // 继续执行原方法，并返回其结果
            return pjp.proceed();
        } catch (Throwable e) {
            // 异常时清理 Redis 键
            stringRedisTemplate.delete(key);
            log.error("NoRepeatSubmitAspect around, error: ", e);
            throw e;
        }
    }

    /**
     * 构造一个用于防止表单重复提交的唯一键
     *
     * @return 唯一键值，格式为"noRepeatSubmit:sessionId:请求URI"，用于标识每个请求的唯一性
     */
    private String getKey() {
        // 获取会话ID，用于区分不同的用户会话
        String sessionId = httpServletRequest.getSession().getId();
        // 获取请求的URI，用于区分不同的请求路径
        String uri = httpServletRequest.getRequestURI();
        // 拼接会话ID和请求URI，生成唯一键
        return RedisKey.getKey(RedisKey.NO_REPEAT_SUBMIT_PREFIX, sessionId,uri);
    }

}