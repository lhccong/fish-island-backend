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
                .header("cookie", "_zap=06bee125-d912-48bb-9268-a20bc0e082f4; d_c0=AACSIeP7GxmPTrLfEYb8pFFlDMCr0B6-pgY=|1724154699; q_c1=f2a6f5588c8b4405995f66908ca721c0|1740453050000|1740453050000; Hm_lvt_98beee57fd2ef70ccdd5ca52b9740c49=1745735105,1745800809,1746276491,1746581413; _xsrf=4oOG5JqxShVAVjgLpKVuKeHrDPXjc4Vd; __zse_ck=004_rqM8rNfuKlFVYGEFyEGXy3HvGveq5dBUO4uX485qzLLc2xtArTW8zRuivVqEe3SQ6mkVa711cZmV0/vN0rHxG7eYY3TPHgvaDGevtNp3TXh9MZGqxurCeNRK2VYNnKfD-A2YwS3mqgWSx2Fe5mJdUNH6YsPtvlDhmZdTpwuiFCDaY+Et0Vh15ypETwzvUhUvjld4yFQ0piZBudUJ0z5kl5UdVKa8bqX8vHclDV8cBf+eClTXkkWzGmYVdxzJpll/I; z_c0=2|1:0|10:1756277296|4:z_c0|80:MS4xUThEb0R3QUFBQUFtQUFBQVlBSlZUVEQ0bTJtNUlpbzUzSnZhUkdJbDNHV3ROR1ZFX3pfcmpRPT0=|3a91b975e94912707907e6f8715a3d09829b5e330e4c5b761731f8337cea45c0; gdxidpyhxdE=8rIChJohw55xBadzIeo2vdoi%5ChVg3wAv2Cv9NCCKnxNznoh4qItNA9vVWBRQpYaVR8yygvYIE2%2BV%5C%2FyXPnC4zt4YBKVevDgEikr%2BvTzsWznB4%2BSjSyDt0XdwqvAqmM%2BTAgjLH4NBqhGrOvC4Mj5dfdgAnm%2F8ljCs5xDGvHxWVz81cR05%3A1756278196996; SESSIONID=lXPTcskcGjhNilhunqfKRvk8qf2K2gM92jVXdtUwYoK; BEC=32377ec81629ec05d48c98f32428ae46")
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
