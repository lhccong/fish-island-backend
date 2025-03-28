package com.github.yuyuanweb.mianshiyaplugin.api.interceptor;

import com.github.yuyuanweb.mianshiyaplugin.config.GlobalState;
import com.github.yuyuanweb.mianshiyaplugin.constant.CommonConstant;
import com.github.yuyuanweb.mianshiyaplugin.model.enums.ErrorCode;
import com.github.yuyuanweb.mianshiyaplugin.model.common.BaseResponse;
import com.github.yuyuanweb.mianshiyaplugin.view.LoginPanel;
import com.google.gson.Gson;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.ProjectManager;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * 响应拦截器
 *
 * @author pine
 */
public class ResponseInterceptor implements Interceptor {

    private final static Gson GSON = new Gson();

    private static final Set<String> NO_NEED_LOGIN_PANEL = new HashSet<>(){{
        add("api/user/get/login");
    }};

    @NotNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Response response = chain.proceed(originalRequest);
        MediaType mediaType = response.body().contentType();

        ResponseBody body = response.body();
        if (body == null) {
            return chain.proceed(originalRequest);
        }
        // body.string() 调用后会把响应体清空，所以后面要重新构建响应体
        String data = body.string();
        BaseResponse<?> baseResponse = GSON.fromJson(data, BaseResponse.class);
        String apiPath = originalRequest.url().toString().replace(CommonConstant.HOST, "");
        if (NO_NEED_LOGIN_PANEL.contains(apiPath)) {
            return chain.proceed(originalRequest);
        }
        if (baseResponse.getCode() == ErrorCode.NOT_LOGIN_ERROR.getCode()) {
            ApplicationManager.getApplication().invokeAndWait(() -> {
                LoginPanel loginPanel = new LoginPanel(ProjectManager.getInstance().getDefaultProject());
                loginPanel.showAndGet();
            });
            originalRequest = originalRequest.newBuilder().header("Cookie", GlobalState.getInstance().getSavedCookie()).build();
            return chain.proceed(originalRequest);
        }

        return response.newBuilder()
                .body(ResponseBody.create(mediaType, data))
                .build();
    }
}
