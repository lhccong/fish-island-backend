package com.github.yuyuanweb.mianshiyaplugin.config;

import com.github.yuyuanweb.mianshiyaplugin.adapter.DateTypeAdapter;
import com.github.yuyuanweb.mianshiyaplugin.api.MianShiYaApi;
import com.github.yuyuanweb.mianshiyaplugin.api.interceptor.HeaderInterceptor;
import com.github.yuyuanweb.mianshiyaplugin.api.interceptor.LogInterceptor;
import com.github.yuyuanweb.mianshiyaplugin.api.interceptor.ResponseInterceptor;
import com.github.yuyuanweb.mianshiyaplugin.constant.CommonConstant;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.Date;

/**
 * 初始化请求工具
 *
 * @author pine
 */
public class ApiConfig {

    public static MianShiYaApi mianShiYaApi;

    static {
        // 自定义 Gson 实例
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .create();
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new HeaderInterceptor())
                .addInterceptor(new LogInterceptor())
                .addInterceptor(new ResponseInterceptor())
                .build();
        String mianShiYaBaseUrl = CommonConstant.API;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mianShiYaBaseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build();

        mianShiYaApi = retrofit.create(MianShiYaApi.class);
    }

}
