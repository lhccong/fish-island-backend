package com.cong.fishisland.datasource.hostpost;

import com.cong.fishisland.model.vo.hot.HotPostDataVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * NGA 版面主题列表抓取与解析（GBK），按 fid 复用于多个版面。
 */
@Slf4j
@Component
public class NgaForumFetcher {

    static final String BASE_URL = "https://bbs.nga.cn";
    private static final Pattern TID_IN_HREF = Pattern.compile("tid=(\\d+)");
    /** 版面主题表：每个 tbody 一行主题 */
    private static final String CSS_TOPIC_ROWS = "#topicrows tbody";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /**
     * 抓取指定版面并解析为帖子列表，按回复数排序后取前 topN 条。
     *
     * @param fid      版面 fid，如杂谈 -7、晴风村 -7955747
     * @param ngaCookie 可选 Cookie，可为空
     * @param topN     返回条数上限
     */
    public List<HotPostDataVO> fetchTopByReplies(int fid, String ngaCookie, int topN) {
        String listUrl = threadListUrl(fid);
        Document document = fetchDocument(listUrl, ngaCookie);
        if (document == null) {
            return Collections.emptyList();
        }
        List<HotPostDataVO> dataList = parseTopicRows(document);
        if (dataList.isEmpty()) {
            return Collections.emptyList();
        }
        return dataList.stream()
                .sorted(Comparator.<HotPostDataVO>comparingInt(
                        h -> h.getFollowerCount() != null ? h.getFollowerCount() : 0).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    public static String threadListUrl(int fid) {
        return BASE_URL + "/thread.php?fid=" + fid;
    }

    private Document fetchDocument(String listUrl, String ngaCookie) {
        try {
            Connection conn = Jsoup.connect(listUrl)
                    .userAgent(USER_AGENT)
                    .timeout(20_000)
                    .maxBodySize(0)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    .header("Referer", listUrl);
            if (StringUtils.isNotBlank(ngaCookie)) {
                conn.header("Cookie", ngaCookie);
            }
            byte[] body = conn.execute().bodyAsBytes();
            return Jsoup.parse(new String(body, Charset.forName("GBK")), listUrl);
        } catch (Exception e) {
            log.error("获取 NGA 版面页面失败: {}", listUrl, e);
            return null;
        }
    }

    private List<HotPostDataVO> parseTopicRows(Document document) {
        List<HotPostDataVO> dataList = new ArrayList<>();
        Set<String> seenTid = new HashSet<>();

        for (Element tbody : document.select(CSS_TOPIC_ROWS)) {
            Element topicA = tbody.selectFirst("a.topic");
            if (topicA == null) {
                continue;
            }
            String title = topicA.text().trim();
            if (title.isEmpty() || title.contains("帖子发布或回复时间超过限制")) {
                continue;
            }
            String href = topicA.attr("href").trim();
            Matcher tidMatcher = TID_IN_HREF.matcher(href);
            if (!tidMatcher.find()) {
                continue;
            }
            String tid = tidMatcher.group(1);
            if (!seenTid.add(tid)) {
                continue;
            }

            String path = href.startsWith("/") ? href : "/" + href;
            String fullUrl = BASE_URL + path;

            int replies = 0;
            Element repliesA = tbody.selectFirst("a.replies");
            if (repliesA != null) {
                try {
                    replies = Integer.parseInt(repliesA.text().trim());
                } catch (NumberFormatException ignored) {
                    // ignore
                }
            }

            dataList.add(HotPostDataVO.builder()
                    .title(title)
                    .url(fullUrl)
                    .followerCount(replies)
                    .build());
        }
        return dataList;
    }
}
