package com.cong.fishisland.datasource.hostpost;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.cong.fishisland.model.entity.hot.HotPost;
import com.cong.fishisland.model.enums.CategoryTypeEnum;
import com.cong.fishisland.model.enums.UpdateIntervalEnum;
import com.cong.fishisland.model.vo.hot.HotPostDataVO;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 吾爱破解 热榜数据源
 *
 * @author itzixiao
 */
@Slf4j
@Component
public class WuAiPoJieDataSource implements DataSource {

    private static final String PJ_HOT_URL = "https://www.52pojie.cn/misc.php?mod=ranklist&type=thread&view=heats&orderby=today";

    @Override
    public HotPost getHotPost() {
        List<HotPostDataVO> allDataList = new ArrayList<>();
        try {
            String result = HttpRequest.get(PJ_HOT_URL).execute().body();
            if (result == null) {
                log.error("吾爱破解 热榜数据获取失败");
                return null;
            }

            Document doc = Jsoup.parse(result);

            Elements rows = doc.select("table tr");
            for (Element row : rows) {
                Element a = row.selectFirst("th > a[target=_blank]");
                Element hotTd = row.select("td").last(); // 最后一列是热度
                if (a != null && hotTd != null) {
                    String title = a.text();
                    String url = a.absUrl("href");
                    if (url == null || url.isEmpty()) {
                        url = "https://www.52pojie.cn/" + a.attr("href");
                    }
                    int followerCount = 0;
                    try {
                        followerCount = Integer.parseInt(hotTd.text());
                    } catch (Exception ignore) {
                    }
                    HotPostDataVO vo = HotPostDataVO.builder()
                            .title(title)
                            .url(url)
                            .followerCount(followerCount)
                            .build();
                    allDataList.add(vo);
                }
            }
        } catch (Exception e) {
            log.error("吾爱破解 热榜数据获取失败", e);
        }

        return HotPost.builder()
                .sort(CategoryTypeEnum.TECH_PROGRAMMING.getValue())
                .category(CategoryTypeEnum.TECH_PROGRAMMING.getValue())
                .name("吾爱破解热榜")
                .updateInterval(UpdateIntervalEnum.HALF_HOUR.getValue())
                .iconUrl("https://www.52pojie.cn/favicon.ico")
                .hostJson(JSON.toJSONString(allDataList)) // 这里放你的数据
                .typeName("吾爱破解")
                .build();
    }

}