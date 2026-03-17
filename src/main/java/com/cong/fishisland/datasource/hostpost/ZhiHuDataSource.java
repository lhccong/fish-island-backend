package com.cong.fishisland.datasource.hostpost;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cong.fishisland.model.entity.hot.HotPost;
import com.cong.fishisland.model.enums.CategoryTypeEnum;
import com.cong.fishisland.model.enums.UpdateIntervalEnum;
import com.cong.fishisland.model.vo.hot.HotPostDataVO;
import com.cong.fishisland.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知乎热榜数据源
 *
 * @author cong
 * @date 2025/02/21
 */
@Slf4j
@Component
public class ZhiHuDataSource implements DataSource {

    @Value("${fishisland.datasource.zhihu.cookie:}")
    private String zhihuCookie;

    @Override
    public HotPost getHotPost() {
        String urlZhiHu = "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total?limit=50&desktop=true";
        //带上请求头
        HttpRequest request = HttpRequest.get(urlZhiHu)
                .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("referer", "https://www.zhihu.com/hot")
                .header("accept", "application/json, text/plain, */*")
                .header("accept-language", "zh-CN,zh;q=0.9");
        if (org.springframework.util.StringUtils.hasText(zhihuCookie)) {
            request.header("cookie", zhihuCookie);
        }
        String result = request.execute().body();
        JSONObject resultJson = (JSONObject) JSON.parse(result);
        JSONArray data = resultJson.getJSONArray("data");
        List<HotPostDataVO> dataList = data.stream().map(item -> {
            JSONObject jsonItem = (JSONObject) item;
            JSONObject target = jsonItem.getJSONObject("target");
            String title = target.getString("title");
            String[] parts = target.getString("url").split("/");
            String url = "https://zhihu.com/question/" + parts[parts.length - 1];
            String followerCount = jsonItem.getString("detail_text");

            return HotPostDataVO.builder()
                    .title(title)
                    .url(url)
                    .followerCount(Integer.parseInt(StringUtils.extractNumber(followerCount)) * 10000)
                    .build();
        }).collect(Collectors.toList());
        return HotPost.builder()
                .sort(CategoryTypeEnum.GENERAL_DISCUSSION.getValue())
                .category(CategoryTypeEnum.GENERAL_DISCUSSION.getValue())
                .name("知乎热榜")
                .updateInterval(UpdateIntervalEnum.HALF_HOUR.getValue())
                .iconUrl("https://www.zhihu.com/favicon.ico")
                .hostJson(JSON.toJSONString(dataList.subList(0, Math.min(dataList.size(), 20))))
                .typeName("知乎")
                .build();
    }
}
