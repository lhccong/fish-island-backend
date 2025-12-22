package com.cong.fishisland.service;

import com.cong.fishisland.common.TestBase;
import com.rometools.rome.feed.synd.SyndEntry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 使用真实 RSS 地址验证 Rome 解析是否正常
 */
@Slf4j
public class RssFeedServiceTest extends TestBase {

    private static final String TEST_RSS_URL =
            "https://justlovemaki.github.io/CloudFlare-AI-Insight-Daily/rss.xml";

    @Autowired
    private RssFeedService rssFeedService;

    @Test
    public void testFetchEntriesFromRealRss() {
        List<SyndEntry> entries = rssFeedService.fetchEntries(TEST_RSS_URL);

        // 基础断言：列表不为 null
        Assertions.assertNotNull(entries, "RSS entries should not be null");

        // 如果解析成功，通常会有至少一条记录
        Assertions.assertTrue(entries.size() > 0, "RSS entries should not be empty");

        // 打印前几条日志（包含标题、链接和摘要），方便调试查看
        entries.stream()
                .limit(1)
                .forEach(entry -> {
                    String description = entry.getDescription() != null
                            ? entry.getDescription().getValue()
                            : "";
                    log.info("title={}, link={}, description={}", entry.getTitle(), entry.getLink(), description);
                });
    }
}


