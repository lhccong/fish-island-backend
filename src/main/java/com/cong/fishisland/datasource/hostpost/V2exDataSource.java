package com.cong.fishisland.datasource.hostpost;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cong.fishisland.model.entity.hot.HotPost;
import com.cong.fishisland.model.enums.CategoryTypeEnum;
import com.cong.fishisland.model.enums.UpdateIntervalEnum;
import com.cong.fishisland.model.vo.hot.HotPostDataVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * V2EX热榜数据源
 * <p>
 * 数据来源：V2EX官方API
 * 更新频率：每30分钟
 * 分类：技术 & 编程
 * </p>
 *
 * @author JackyST0
 * @date 2026/01/16
 */
@Slf4j
@Component
public class V2exDataSource implements DataSource {

    /**
     * V2EX热门话题API地址
     */
    private static final String API_URL = "https://www.v2ex.com/api/topics/hot.json";
    
    /**
     * 请求超时时间（毫秒）
     */
    private static final int TIMEOUT = 10000;

    @Override
    public HotPost getHotPost() {
        List<HotPostDataVO> dataList = new ArrayList<>();
        
        try {
            // 发送HTTP请求获取JSON数据
            String jsonResult = HttpRequest.get(API_URL)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(TIMEOUT)
                    .execute()
                    .body();

            // 解析JSON数组
            JSONArray topics = JSON.parseArray(jsonResult);
            
            // 只取前20条
            int limit = Math.min(topics.size(), 20);
            
            for (int i = 0; i < limit; i++) {
                JSONObject topic = topics.getJSONObject(i);
                
                // 提取数据
                String title = topic.getString("title");
                String url = topic.getString("url");
                Integer replies = topic.getInteger("replies");
                
                // 提取节点信息作为摘要
                String nodeName = "";
                if (topic.containsKey("node")) {
                    JSONObject node = topic.getJSONObject("node");
                    nodeName = node.getString("title");
                }
                
                // 构建热榜数据
                dataList.add(HotPostDataVO.builder()
                        .title(title)
                        .url(url)
                        .followerCount(replies != null ? replies : 0)
                        .excerpt(nodeName) // 节点名称作为摘要
                        .build());
            }
            
            log.info("成功获取V2EX热榜数据，共{}条", dataList.size());
            
        } catch (Exception e) {
            log.error("获取V2EX热榜失败: {}", e.getMessage(), e);
            // 抓取失败时返回空数据，不影响其他热榜
            return buildEmptyHotPost();
        }

        return buildHotPost(dataList);
    }

    /**
     * 构建热榜数据对象
     */
    private HotPost buildHotPost(List<HotPostDataVO> dataList) {
        return HotPost.builder()
                .sort(CategoryTypeEnum.TECH_PROGRAMMING.getValue())
                .category(CategoryTypeEnum.TECH_PROGRAMMING.getValue())
                .name("V2EX热榜")
                .updateInterval(UpdateIntervalEnum.HALF_HOUR.getValue())
                .iconUrl("https://www.v2ex.com/static/img/icon_rayps_64.png")
                .hostJson(JSON.toJSONString(dataList))
                .typeName("V2EX")
                .build();
    }

    /**
     * 构建空热榜数据对象（抓取失败时使用）
     */
    private HotPost buildEmptyHotPost() {
        return HotPost.builder()
                .sort(CategoryTypeEnum.TECH_PROGRAMMING.getValue())
                .category(CategoryTypeEnum.TECH_PROGRAMMING.getValue())
                .name("V2EX热榜")
                .updateInterval(UpdateIntervalEnum.HALF_HOUR.getValue())
                .iconUrl("https://www.v2ex.com/static/img/icon_rayps_64.png")
                .hostJson(JSON.toJSONString(new ArrayList<>()))
                .typeName("V2EX")
                .build();
    }
}
