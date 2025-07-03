package com.cong.fishisland.datasource.hostpost;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cong.fishisland.model.entity.hot.HotPost;
import com.cong.fishisland.model.enums.CategoryTypeEnum;
import com.cong.fishisland.model.enums.UpdateIntervalEnum;
import com.cong.fishisland.model.vo.hot.HotPostDataVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 百度贴吧热榜
 *
 * @author Shing
 */
@Slf4j
@Component
public class TieBaDataSource implements DataSource{
    @Override
    public HotPost getHotPost() {

        String tiebaHotUrl = "https://tieba.baidu.com/hottopic/browse/topicList";
        String result = HttpUtil.get(tiebaHotUrl);

        List<HotPostDataVO> dataList = parseTiebaTopicList(result);

        List<HotPostDataVO> sortedTopList = dataList.stream()
                .sorted(Comparator.comparingInt(HotPostDataVO::getFollowerCount).reversed())
                .limit(20)
                .collect(Collectors.toList());

        return HotPost.builder()
                .sort(CategoryTypeEnum.GENERAL_DISCUSSION.getValue())
                .name("百度贴吧热榜")
                .category(CategoryTypeEnum.GENERAL_DISCUSSION.getValue())
                .updateInterval(UpdateIntervalEnum.HALF_HOUR.getValue())
                .iconUrl("https://tieba.baidu.com/favicon.ico")
                .hostJson(JSON.toJSONString(sortedTopList))
                .typeName("百度贴吧")
                .build();
    }

    private List<HotPostDataVO> parseTiebaTopicList(String json) {
        JSONObject resultJson = JSON.parseObject(json);
        JSONArray topicList = Optional.ofNullable(resultJson)
                .map(result  -> result .getJSONObject("data"))
                .map(data  -> data .getJSONObject("bang_topic"))
                .map(bangTopic  -> bangTopic .getJSONArray("topic_list"))
                .orElseThrow(() -> new RuntimeException("数据结构异常，未获取到 topic_list"));

        return topicList.stream().map(item -> {
            JSONObject jsonItem = (JSONObject) item;
            return HotPostDataVO.builder()
                    .title(jsonItem.getString("topic_name"))
                    .url(jsonItem.getString("topic_url"))
                    .followerCount(jsonItem.getIntValue("discuss_num"))
                    .build();
        }).collect(Collectors.toList());
    }
}
