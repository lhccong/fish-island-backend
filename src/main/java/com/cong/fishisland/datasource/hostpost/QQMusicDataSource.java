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

    private static final String musicDetailUrl = "https://y.qq.com/n/ryqq/songDetail/";


    //提交
    @Override
    public HotPost getHotPost() {

        //调用接口 https://u.y.qq.com/cgi-bin/musicu.fcg?data=，GET请求，最终获取到json格式的字符串数据，需要传data,data为json格式的字符串，生成一下以下代码
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

        List<HotPostDataVO> hotPostDataVOS = new ArrayList<>();
        // 3. 遍历数组，提取 title 和 songId
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject song = jsonArray.getJSONObject(i);
            String title = song.getString("title");
            long songId = song.getLongValue("songId"); // 或 getLong("songId")
            Integer rank = song.getInteger("rank");
            hotPostDataVOS.add(HotPostDataVO.builder().title(title).url(musicDetailUrl + songId).followerCount(rank).build());
        }

        return HotPost.builder()
                .sort(CategoryTypeEnum.MUSIC_HOT.getValue())
                .category(CategoryTypeEnum.MUSIC_HOT.getValue())
                .name("QQ音乐热歌榜")
                .updateInterval(UpdateIntervalEnum.TWO_HOUR.getValue())
                .iconUrl("https://api.oss.cqbo.com/moyu/user_avatar/1922893849325314049/ciGXlg1M-logo.png")
                .hostJson(JSON.toJSONString(hotPostDataVOS.subList(0, Math.min(hotPostDataVOS.size(), 20))))
                .typeName("QQ音乐")
                .build();

    }

}

