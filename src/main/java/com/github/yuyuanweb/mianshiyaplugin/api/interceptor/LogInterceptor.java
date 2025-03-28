package com.github.yuyuanweb.mianshiyaplugin.api.interceptor;

import com.intellij.openapi.diagnostic.Logger;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * 请求拦截器
 *
 * @author pine
 */
public class LogInterceptor implements Interceptor {

    private static final Logger logger = Logger.getInstance(LogInterceptor.class);

    @NotNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        logger.warn(originalRequest.method() + " " + originalRequest.url());
        return chain.proceed(originalRequest);
    }

}
