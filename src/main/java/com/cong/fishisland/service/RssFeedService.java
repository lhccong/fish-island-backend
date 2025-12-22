package com.cong.fishisland.service;

import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 使用 Rome 解析 RSS / Atom 订阅源的简单服务
 */
@Service
@Slf4j
public class RssFeedService {

    /**
     * 根据给定的 RSS 地址拉取并解析订阅条目列表
     *
     * @param rssUrl RSS 地址
     * @return 订阅条目列表；解析失败时返回空列表
     */
    public List<SyndEntry> fetchEntries(String rssUrl) {
        try {
            URL url = new URL(rssUrl);
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(url));
            if (feed.getEntries() == null) {
                return Collections.emptyList();
            }
            // 复制一份 List，避免返回 Rome 内部的可修改集合
            return new ArrayList<>(feed.getEntries());
        } catch (IllegalArgumentException | FeedException | IOException e) {
            log.error("Failed to fetch or parse RSS feed from url={}", rssUrl, e);
            return Collections.emptyList();
        }
    }
}





