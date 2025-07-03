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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * csdn 热榜数据源
 *
 * @author Shing
 */
@Slf4j
@Component
public class CsdnDataSource implements DataSource {

    private static final String CSDN_HOT_URL = "https://blog.csdn.net/phoenix/web/blog/hot-rank";

    private static final String CSDN_COOKIE = "UN=LHCong_; p_uid=U010000; fid=20_61687862040-1723984475179-679236; Hm_ct_6bcd52f51e9b3dce32bec4a3997715ac=6525*1*10_20051545390-1719703490935-626132!5744*1*LHCong_; c_dl_um=-; uuid_tt_dd=10_9896516350-1739327625592-841658; UserName=LHCong_; UserInfo=f43d02c96baf4ddfb4f79b3bc8205af1; UserToken=f43d02c96baf4ddfb4f79b3bc8205af1; UserNick=%E8%81%AA%CE%B6; AU=347; BT=1739945247083; c_dl_prid=1743562454029_761266; c_dl_rid=1744005437590_844922; c_dl_fref=https://blog.csdn.net/qq_43592352/article/details/104228198; c_dl_fpage=/download/qq_43592352/12367798; ssxmod_itna=eqIOBKGKAIPmxmuxBa8QrEeDIZ3rCCY=gDGqKDsqdTDSxGKidDqxBnmC+gFgIu+=Itm3YxQiCjOp0pqbUnW4YEm=0u+ftQGO4GIDeKG2DmeDyDi5GRD09eWD4RKGwD0eG+DD4DWGqDtV7T=D7oNgjTNXWq=07dNDmb=uDGQcDiUdxi5qxW0ll8GlDD3dxB64xA3uD7tVWTQDbqDuGa6ouqDLUCa7dcQDbopg36WDtuuueqDHBMXDaMybbYP5b7+ztO+j37DTGGq4D0KLx8GYbYCq8rC=GGDhFBAdSOzr1G4SPD==; ssxmod_itna2=eqIOBKGKAIPmxmuxBa8QrEeDIZ3rCCY=gDGqikAqt6Dl1D7uK03e8njXqggtUqfxoyxHhjpGAd5djriOdmg+xlG=zqGDnFUPUfZCDaD6BGFTeUKuuj7cl4HCCjE35yn3x4sEakhCTAEjD1iUvFLKR7ri44Q6xPe4S3l=SUdoyFLE2tR0D5q49mTQK=uhZKqexH7bgK8=zYqvWLhbt6fjmQDPDKkqHD7=DekqxD==; tfstk=gZ9tAgbVjDEtOOXoIAG3iCPfseonrKKaRF-7nZbgGeLpzezGiEXGHiQpuRvDmnOYH37enN2MmmLvoU_1j1DNAwslXiw1fsTddiI4GPw_1kZwpUe0IVlwc-Bch40oEYxabtWjrJw6FAQN0H_jx1GNxH1Gh40oKigWE06XSJAi1kKC8iIfcsTbA9_F2r6f5saQOg_CltTf59NCqiP_liN1ODIVRZ6fhEtI6Z0ARl_0HQJ4Stz42G2bhpIOOXx1R57WpGCOPhTae8F5X1QWfw3yNa6PMFC9KAUPXBtJSGLqURW1MCO5CK3-B9tytFspl4FARQpwH_vty776QZ8RCB3QBt66BQfPHbENxdKBUspZWJ_98HOlQLgUHUtHqd5eH4UCuCjPdGdKcW_1Gg8wELC1IzbRm5iKvSPV1MzWzY0qMpeCpMQoXxF4g6oUn5pozSNbzajdrDx7gS5EY; Hm_lvt_ec8a58cd84a81850bcbd95ef89524721=1741580928,1741915206,1744005547; Hm_lvt_6bcd52f51e9b3dce32bec4a3997715ac=1745733865,1745755675,1745890661,1746581158; _clck=1ek7vus%7C2%7Cfvq%7C0%7C1641; _ga=GA1.1.1399695897.1719703493; _ga_7W1N0GEY1P=GS2.1.s1746670523$o128$g1$t1746672278$j54$l0$h0; csdn_newcert_LHCong_=1; c_adb=1; https_waf_cookie=c30938ff-57a9-4bed9dd6f13587d5693be90a26a6223538f8; c_first_ref=www.google.com; c_segment=10; dc_sid=3b13d0c43d904e69580ae439f6d9de7f; c_pref=https%3A//blog.csdn.net/weixin_43829930; c_ref=https%3A//www.google.com/; c_first_page=https%3A//blog.csdn.net/shujuwa_data/article/details/128913638; bc_bot_session=17501224931db0644da9d9e870; waf_captcha_marker=f93258221a343c7c64f6786fda6d98fa50e0159a4ef2be79f28cc50ec6b977f5; dc_session_id=10_1750122495762.596118; yd_captcha_token=MTc1MDEyMjQ5ODc5OF81OC4yNTIuMjI2LjE3OF85MjA3ZWM2ZjdlZjZmNzdlMjM1MjhiMTBkZmZjM2QxODUwYQ%3D%3D";

    @Override
    public HotPost getHotPost() {
        int pageSize = 25;
        int pagesNeeded = 3;
        List<HotPostDataVO> allDataList = new ArrayList<>();

        for (int page = 0; page < pagesNeeded; page++) {
            String url = CSDN_HOT_URL + "?page=" + page + "&pageSize=" + pageSize + "&type=";

            try {
                // 发送 GET 请求并获取 JSON 响应
                String result = HttpRequest.get(url)
                        .header("Cookie", CSDN_COOKIE)
                        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                        .header("Referer", "https://blog.csdn.net/")
                        .execute().body();
                JSONObject resultJson = JSON.parseObject(result);
                JSONArray data = resultJson.getJSONArray("data");
                // 解析数据并转换为 VO 对象
                List<HotPostDataVO> dataList = data.stream().map(item -> {
                    JSONObject jsonItem = (JSONObject) item;
                    String title = Optional.ofNullable(jsonItem.getString("articleTitle")).orElse("");
                    String articleDetailUrl = jsonItem.getString("articleDetailUrl");
                    String hotRankScore = Optional.ofNullable(jsonItem.getString("hotRankScore")).orElse("0");

                    return HotPostDataVO.builder()
                            .title(title)
                            .url(articleDetailUrl)
                            .followerCount(parseHotRankScore(hotRankScore))
                            .build();
                }).collect(Collectors.toList());
                allDataList.addAll(dataList);
            } catch (Exception e) {
                log.error("CSDN 热榜数据获取失败", e);
            }

        }

        List<HotPostDataVO> sortedDataList = allDataList.stream()
                .sorted(Comparator.comparingInt(HotPostDataVO::getFollowerCount).reversed())
                .collect(Collectors.toList());

        return HotPost.builder()
                .sort(CategoryTypeEnum.TECH_PROGRAMMING.getValue())
                .category(CategoryTypeEnum.TECH_PROGRAMMING.getValue())
                .name("CSDN热榜")
                .updateInterval(UpdateIntervalEnum.HALF_HOUR.getValue())
                .iconUrl("https://blog.csdn.net/favicon.ico")
                // 取前 20 条数据
                .hostJson(JSON.toJSONString(sortedDataList.subList(0, Math.min(sortedDataList.size(), 20))))
                .typeName("CSDN")
                .build();
    }

    /**
     * 解析 hotRankScore，确保其为整数
     *
     * @param hotRankScore 纯数字字符串
     * @return 转换后的整数值
     */
    private int parseHotRankScore(String hotRankScore) {
        try {
            return Integer.parseInt(hotRankScore);
        } catch (NumberFormatException e) {
            log.warn("Invalid hotRankScore format: {}", hotRankScore);
            return 0;
        }
    }
}