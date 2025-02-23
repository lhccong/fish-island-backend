package com.cong.fishisland.datasource;


import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cong.fishisland.model.entity.hot.HotPost;
import com.cong.fishisland.model.vo.hot.HotPostDataVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * BiliBili 数据源
 *
 * @author shing
 */
@Slf4j
@Component
public class BilibiliDataSource implements DataSource {

    @Override
    public HotPost getHotPost() {
        String urlBilibili = "https://api.bilibili.com/x/web-interface/popular";
        String result = HttpRequest.get(urlBilibili).execute().body();
        JSONObject resultJson = JSON.parseObject(result);

        // 获取嵌套数据结构
        JSONObject data = resultJson.getJSONObject("data");
        JSONArray list = data.getJSONArray("list");

        List<HotPostDataVO> dataList = list.stream().map(item -> {
            JSONObject jsonItem = (JSONObject) item;

            // 提取基础字段
            String title = jsonItem.getString("title");
            String desc = jsonItem.getString("desc");
            String shortLink = jsonItem.getString("short_link_v2");

            // 提取嵌套的统计信息
            JSONObject stat = jsonItem.getJSONObject("stat");
            int view = stat.getIntValue("view");

            return HotPostDataVO.builder()
                    .title(title)
                    .url(shortLink)
                    .excerpt(desc)
                    .followerCount(view)
                    .build();
        }).collect(Collectors.toList());

        return HotPost.builder()
                .name("B站热门")
                .iconUrl("https://www.bilibili.com/favicon.ico")  // 修正图标地址
                .hostJson(JSON.toJSONString(dataList))
                .typeName("bilibili")
                .build();
    }

}
