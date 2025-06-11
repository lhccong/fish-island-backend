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

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class QQMusicDataSource implements DataSource {


    private static final String MUSIC_DETAIL_URL = "https://y.qq.com/n/ryqq/songDetail/";

    //提交
    @Override
    public HotPost getHotPost() {

        String jsonData = "{\n" +
                "        \"detail\": {\n" +
                "            \"module\": \"musicToplist.ToplistInfoServer\",\n" +
                "                    \"method\": \"GetDetail\",\n" +
                "                    \"param\": {\n" +
                "                \"topId\": 26,\n" +
                "                        \"offset\": 0,\n" +
                "                        \"num\": 20,\n" +
                "                        \"period\": \"20250606\"\n" +
                "            }\n" +
                "        }\n" +
                "    }";
        //发送请求
        String response = HttpUtil.get("https://u.y.qq.com/cgi-bin/musicu.fcg?data=" + jsonData);

//        log.info("获取数据成功：{}", response);

        JSONObject jsonObject = JSON.parseObject(response);

//        System.out.println(jsonObject);
        JSONArray jsonArray = jsonObject.getJSONObject("detail").getJSONObject("data").getJSONObject("data").getJSONArray("song");

        List<HotPostDataVO> hotPostDataVos = new ArrayList<>();
        // 3. 遍历数组，提取 title 和 songId
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject song = jsonArray.getJSONObject(i);
            String title = song.getString("title");
            long songId = song.getLongValue("songId");
            Integer rank = song.getInteger("rank");
            hotPostDataVos.add(HotPostDataVO.builder().title(title).url(MUSIC_DETAIL_URL + songId).followerCount(rank).build());
        }

        return HotPost.builder()
                .sort(CategoryTypeEnum.MUSIC_HOT.getValue())
                .category(CategoryTypeEnum.MUSIC_HOT.getValue())
                .name("QQ音乐热歌榜")
                .updateInterval(UpdateIntervalEnum.TWO_HOUR.getValue())
                .iconUrl("https://s1.aigei.com/src/img/png/5d/5d7d335b9cab49e39236418edf31ed1f.png?imageMogr2/auto-orient/thumbnail/!282x282r/gravity/Center/crop/282x282/quality/85/%7CimageView2/2/w/282&e=2051020800&token=P7S2Xpzfz11vAkASLTkfHN7Fw-oOZBecqeJaxypL:lwKuad6Nw3hxdwVBE-_6NeCuqf0=")
                .hostJson(JSON.toJSONString(hotPostDataVos.subList(0, Math.min(hotPostDataVos.size(), 20))))
                .typeName("QQ音乐")
                .build();

    }

}

