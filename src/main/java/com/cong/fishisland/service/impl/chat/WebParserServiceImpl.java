package com.cong.fishisland.service.impl.chat;

import com.cong.fishisland.model.vo.WebParseVO;
import com.cong.fishisland.service.WebParserService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author cong
 */
@Service
@Slf4j
public class WebParserServiceImpl implements WebParserService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .followRedirects(true)
            .connectTimeout(2, TimeUnit.SECONDS)
            .build();

    @Override
    public WebParseVO parseWebPage(String url) {
        WebParseVO result = new WebParseVO();
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected response " + response);
                }

                assert response.body() != null;
                String html = response.body().string();
                Document doc = Jsoup.parse(html);

                // 获取标题
                String title = doc.title();
                result.setTitle(title.isEmpty() ? "" : title);

                // 获取描述
                Element descriptionMeta = doc.select("meta[name=description]").first();
                String description = descriptionMeta != null ? descriptionMeta.attr("content") : "";
                result.setDescription(description);

                // 获取favicon
                String favicon = "";
                Elements faviconElements = doc.select("link[rel~=icon]");
                if (!faviconElements.isEmpty()) {
                    favicon = Objects.requireNonNull(faviconElements.first()).attr("href");
                    if (!favicon.startsWith("http")) {
                        favicon = new URL(new URL(url), favicon).toString();
                    }
                } else {
                    favicon = new URL(new URL(url), "/favicon.ico").toString();
                }
                result.setFavicon(favicon);
            }
        } catch (Exception e) {
            log.error("Error parsing webpage: " + url, e);
            return new WebParseVO();
        }
        return result;
    }
} 