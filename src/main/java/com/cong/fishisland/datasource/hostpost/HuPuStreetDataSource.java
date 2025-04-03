package com.cong.fishisland.datasource.hostpost;

import com.alibaba.fastjson.JSON;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.entity.hot.HotPost;
import com.cong.fishisland.model.enums.CategoryTypeEnum;
import com.cong.fishisland.model.enums.UpdateIntervalEnum;
import com.cong.fishisland.model.vo.hot.HotPostDataVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 虎扑数据源
 *
 * @author shing
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HuPuStreetDataSource implements DataSource {

    private static final String HUPU_URL = "https://bbs.hupu.com";
    private static final String USER_AGENT = "Mozilla/5.0(Windows NT 10.0;Win64;x64;rv:66.0)Gecko/20100101 Firefox/66.0";
    String huPPuStreetURL = "https://bbs.hupu.com/all-gambia";

    private final RetryTemplate retryTemplate;

    @Override
    public HotPost getHotPost() {
        return retryTemplate.execute(this::fetchHotPost);
    }

    public HotPost fetchHotPost(RetryContext context) {
        Document document = fetchDocument(huPPuStreetURL);

        if (document == null) {
            log.error("无法获取虎扑步行街网页内容");
            return HotPost.builder().build();
        }

        List<HotPostDataVO> dataList = new ArrayList<>();
        Elements listItems = document.select("div.list-item");

        for (Element listItem : listItems) {
            HotPostDataVO dataVO = extractPostData(listItem);
            if (dataVO != null) {
                dataList.add(dataVO);
            }
            // 如果数据为空，则抛出异常进行重试
            if (dataList.isEmpty()) {
                log.warn("获取数据为空，尝试重试... 剩余次数：{}", context.getRetryCount());
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "获取数据为空");
            }
        }
        return HotPost.builder()
                .sort(CategoryTypeEnum.GENERAL_DISCUSSION.getValue())
                .name("虎扑步行街热榜")
                .category(CategoryTypeEnum.GENERAL_DISCUSSION.getValue())
                .updateInterval(UpdateIntervalEnum.HALF_HOUR.getValue())
                .iconUrl("https://hupu.com/favicon.ico")
                .hostJson(JSON.toJSONString(dataList.stream()
                        .sorted((a, b) -> b.getFollowerCount() - a.getFollowerCount()).collect(Collectors.toList())
                        .subList(0, Math.min(dataList.size(), 20))))
                .typeName("虎扑步行街")
                .build();
    }

    /**
     * 抓取网页内容
     *
     * @param url 网页 URL
     * @return Document 返回网页的 Document 对象
     */
    private Document fetchDocument(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .get();
        } catch (IOException e) {
            log.error("获取网页内容失败，URL: " + url, e);
            return null;
        }
    }

    /**
     * 提取帖子的具体信息
     *
     * @param listItem 每一个帖子项
     * @return 返回 HotPostDataVO 对象
     */
    private HotPostDataVO extractPostData(Element listItem) {
        try {
            String postUrl = listItem.select("a").attr("href");
            // 只有当 postUrl 为相对路径且有效时，才进行拼接并处理
            if (postUrl.trim().isEmpty() || postUrl.equals(huPPuStreetURL)) {
                return null;
            }
            // 拼接完整的帖子 URL
            postUrl = HUPU_URL + postUrl;

            String title = listItem.select("span.t-title").text();
            Integer hot = parseHot(listItem.select("span.t-lights").text());

            return HotPostDataVO.builder()
                    .title(title)
                    .url(postUrl)
                    .followerCount(hot)
                    .build();
        } catch (Exception e) {
            log.error("解析帖子数据失败", e);
            return null;
        }
    }

    /**
     * 解析热度数据
     *
     * @param hotStr 热度字符串
     * @return 返回处理后的热度值
     */
    private Integer parseHot(String hotStr) {
        if (hotStr == null || hotStr.isEmpty()) {
            return 0;
        }
        try {
            // 使用正则去除非数字字符
            String numericHotStr = hotStr.replaceAll("\\D", "").trim();
            // 转换为整数并乘以 1000
            return Integer.parseInt(numericHotStr) * 1000;
        } catch (NumberFormatException e) {
            log.error("解析热度数据失败: {}", hotStr, e);
            return 0;
        }
    }
}

