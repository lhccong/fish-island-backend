package com.cong.fishisland.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cong.fishisland.common.TestBaseByLogin;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class SearchJsonFetcherTest extends TestBaseByLogin {

    @Test
    void bilibiliSearchTest() {

        // 1. 获取网页
        String bilibiliURL = "https://api.bilibili.com/x/web-interface/popular";

        // 发送 HTTP 请求并获取响应
        String result = HttpRequest.get(bilibiliURL).execute().body();

        // 2. 解析 JSON 数据
        JSONObject resultJson = JSON.parseObject(result);
        JSONObject data = resultJson.getJSONObject("data");
        JSONArray list = data.getJSONArray("list");

        // 3. 遍历数据并提取所需字段
        list.forEach(item -> {
            JSONObject jsonItem = (JSONObject) item;
            // 获取 stat 对象
            JSONObject stat = jsonItem.getJSONObject("stat");
            String title = jsonItem.getString("title"); // 标题
            String pic = jsonItem.getString("pic"); // 封面
            String desc = jsonItem.getString("desc"); // 介绍
            String shortLinkV2 = jsonItem.getString("short_link_v2"); // 跳转链接
            // 从 stat 对象中获取子字段
            String favorite = stat.getString("favorite");
            String view = stat.getString("view");

            // 打印结果
            log.info("\n标题：{}，\n封面：{}，\n介绍：{}，\n跳转链接：{},\n收藏数：{},\n观看数：{}",
                    title, pic, desc, shortLinkV2, favorite, view);
        });
    }

    @Test
    void tiebaSearchTest() {

        String tiebaHotUrl = "https://tieba.baidu.com/hottopic/browse/topicList";

        String result = HttpUtil.get(tiebaHotUrl);

        JSONObject resultJson = JSON.parseObject(result);
        JSONObject data = resultJson.getJSONObject("data");
        JSONObject bangTopic = data.getJSONObject("bang_topic");
        JSONArray topicList = bangTopic.getJSONArray("topic_list");

        topicList.forEach(item -> {
            JSONObject jsonItem = (JSONObject) item;
            String title = jsonItem.getString("topic_name");
            String url = jsonItem.getString("topic_url");
            String followerCount = jsonItem.getString("discuss_num");

            System.out.println("标题：" + title + "url: " + url + "followerCount:" + followerCount);

        });
    }

}
