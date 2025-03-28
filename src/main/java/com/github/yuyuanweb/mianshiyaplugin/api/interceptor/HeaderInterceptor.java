package com.github.yuyuanweb.mianshiyaplugin.api.interceptor;

import com.github.yuyuanweb.mianshiyaplugin.config.GlobalState;
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
public class HeaderInterceptor implements Interceptor {
    @NotNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder requestBuilder = originalRequest.newBuilder()
                // 添加 cookie
                .header("Cookie", GlobalState.getInstance().getSavedCookie())
                .header("User-Agent", "jetbrains-plugin");
        Request request = requestBuilder.build();
        return chain.proceed(request);
    }
}
