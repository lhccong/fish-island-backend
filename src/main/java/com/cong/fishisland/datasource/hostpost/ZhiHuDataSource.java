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
    @Override
    public HotPost getHotPost() {
        String urlZhiHu = "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total?limit=50&desktop=true";
        //带上请求头
        String result = HttpRequest.get(urlZhiHu)
                .header("cookie", "_xsrf=VN96XyHRESB738GJVA30aJpWs9iN5DZi; _zap=06bee125-d912-48bb-9268-a20bc0e082f4; d_c0=AACSIeP7GxmPTrLfEYb8pFFlDMCr0B6-pgY=|1724154699; __snaker__id=bLAEX3gz29CFPiwb; q_c1=f2a6f5588c8b4405995f66908ca721c0|1740453050000|1740453050000; Hm_lvt_98beee57fd2ef70ccdd5ca52b9740c49=1745735105,1745800809,1746276491,1746581413; gdxidpyhxdE=O%5CfGjOBWV6SeeE2ssXtyLs2R80djWN9gk%5C8h6vy7NRETBrfyQDqifDxgah%2B%2Bo25IHgAqqNEBu4MVycdViLKNC9ANE%2Fham4RD4gQN0UzNo2UxWTzvnMLzwCO%2BVP%5CAri9PE31XEV5%2F1Hxi5kgJmzDjCn9p%5Cht8EXVePcMhdqTrNW79vCwf%3A1751717751215; tst=r; __zse_ck=004_Fe9=sM6ja8gRHSWDyKx9yI5WoPapqRtuVqGSBdeyHzJlx=Y1LNTq/zquBiKFaJJn88V0JBdeG8D4MvOluWxGyzP0yARfvvomQ/8RkBImiNV3Ure/jTa4GYlgJMYWHMlA-rjw5fTCEiAyk27D7qCIzEvUs4GiULw1eZiosIeqVNr3Zf70UkMHbZcDoViGb6yaQHtkcpyWUs37RuFjOAo5He/ThHf7TVDIOcBzQKU9GMrbBKZWHpakFWO7Wyt2nLfqv; z_c0=2|1:0|10:1753681367|4:z_c0|80:MS4xUThEb0R3QUFBQUFtQUFBQVlBSlZUZGRiZEdsZWlMTlZqLUVLempVM2hJLUpHODhHYXJDdHd3PT0=|a79b29e24421a7e2c3320caf73dea077a125bb8dc9ca41e42036d731b5a1e33e; BEC=f7bc18b707cd87fca0d61511d015686f")
                .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("referer", "https://www.zhihu.com/hot")
                .header("accept", "application/json, text/plain, */*")
                .header("accept-language", "zh-CN,zh;q=0.9")
                .execute()
                .body();
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
