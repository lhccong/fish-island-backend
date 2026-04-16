package com.cong.fishisland.datasource.hostpost;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cong.fishisland.model.entity.hot.HotPost;
import com.cong.fishisland.model.enums.CategoryTypeEnum;
import com.cong.fishisland.model.enums.UpdateIntervalEnum;
import com.cong.fishisland.model.vo.hot.HotPostDataVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 掘金沸点数据源
 *
 * @author shing
 */
@Slf4j
@Component
public class JueJinBoilingDataSource implements DataSource {

    private static final String JUE_JIN_BOILING_URL = "https://api.juejin.cn/recommend_api/v1/short_msg/hot";

    private static final String JUE_JIN_BOILING_DETAIL_URL = "https://juejin.cn/pin/";

    @Override
    public HotPost getHotPost() {
        List<HotPostDataVO> allDataList = new ArrayList<>();

        try {
            // 1. 构建请求URL
            URI url = new URIBuilder(JUE_JIN_BOILING_URL)
                    .addParameter("aid", "2608")
                    .addParameter("uuid", "7547945520874948142")
                    .addParameter("spider", "0")
                    .build();

            // 2. 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("id_type", 4);
            requestBody.put("sort_type", 200);
            requestBody.put("cursor", "0");
            requestBody.put("limit", 20);

            // 3. 发送POST请求
            try (HttpResponse response = HttpRequest.post(url.toString())
                    .header("accept", "*/*")
                    .header("accept-language", "zh-CN,zh;q=0.9")
                    .header("content-type", "application/json")
                    .header("origin", "https://juejin.cn")
                    .header("referer", "https://juejin.cn/")
                    .header("sec-ch-ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\"")
                    .header("sec-ch-ua-mobile", "?0")
                    .header("sec-ch-ua-platform", "\"Windows\"")
                    .header("sec-fetch-dest", "empty")
                    .header("sec-fetch-mode", "cors")
                    .header("sec-fetch-site", "same-site")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36")
                    .body(requestBody.toJSONString())
                    .execute()) {

                String result = response.body();
                JSONObject resultJson = JSON.parseObject(result);
                JSONArray data = resultJson.getJSONArray("data");

                // 4. 解析数据
                data.stream()
                        .map(JSONObject.class::cast)
                        .forEach(jsonItem -> {
                            try {
                                JSONObject msgInfo = jsonItem.getJSONObject("msg_Info");
                                String msgId = jsonItem.getString("msg_id");

                                String content = msgInfo.getString("content");
                                int hotIndex = msgInfo.getIntValue("hot_index");
                                int diggCount = msgInfo.getIntValue("digg_count");
                                int commentCount = msgInfo.getIntValue("comment_count");

                                allDataList.add(HotPostDataVO.builder()
                                        .title(content)
                                        .url(JUE_JIN_BOILING_DETAIL_URL + msgId)
                                        .followerCount(hotIndex)
                                        .build());

                            } catch (Exception e) {
                                log.warn("沸点数据解析失败: {}", jsonItem.toJSONString(), e);
                            }
                        });
            }

        } catch (URISyntaxException e) {
            log.error("URL构造失败: {}", JUE_JIN_BOILING_URL, e);
        } catch (Exception e) {
            log.error("沸点接口未知错误", e);
        }

        // 5. 排序并返回
        return HotPost.builder()
                .category(CategoryTypeEnum.GENERAL_DISCUSSION.getValue())
                .sort(CategoryTypeEnum.GENERAL_DISCUSSION.getValue())
                .name("掘金沸点热榜")
                .updateInterval(UpdateIntervalEnum.HALF_HOUR.getValue())
                .iconUrl("https://lf3-cdn-tos.bytescm.com/obj/static/xitu_juejin_web//static/favicon.ico")
                .hostJson(JSON.toJSONString(allDataList.stream()
                        .sorted(Comparator.comparingInt(HotPostDataVO::getFollowerCount).reversed())
                        .collect(Collectors.toList())
                        .subList(0, Math.min(allDataList.size(), 20))))
                .typeName("掘金沸点")
                .build();
    }
}
