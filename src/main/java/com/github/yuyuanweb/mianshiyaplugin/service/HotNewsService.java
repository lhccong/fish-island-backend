package com.github.yuyuanweb.mianshiyaplugin.service;

import com.github.yuyuanweb.mianshiyaplugin.model.HotNews;
import com.github.yuyuanweb.mianshiyaplugin.model.HotNewsCategory;
import com.github.yuyuanweb.mianshiyaplugin.model.HotNewsResponse;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class HotNewsService {
    private static final String API_URL = "https://moyuapi.codebug.icu/fish/api/hot/list";
    private static final String TOKEN = "Re_9NUm4SkQIt2EHJ_l5u8SZcstzfRVNo5__";
    private final HttpClient httpClient;
    private final Gson gson;

    public HotNewsService() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public Map<String, List<HotNews>> fetchHotNewsByCategory() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("fish-dog-token", TOKEN)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                HotNewsResponse hotNewsResponse = gson.fromJson(response.body(), HotNewsResponse.class);
                Map<String, List<HotNews>> categoryNews = new LinkedHashMap<>();
                
                if (hotNewsResponse != null && hotNewsResponse.getData() != null) {
                    for (HotNewsCategory category : hotNewsResponse.getData()) {
                        if (category.getData() != null) {
                            List<HotNews> newsForCategory = new ArrayList<>();
                            for (HotNews news : category.getData()) {
                                news.setTypeName(category.getTypeName());
                                newsForCategory.add(news);
                            }
                            categoryNews.put(category.getTypeName(), newsForCategory);
                        }
                    }
                }
                return categoryNews;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return new LinkedHashMap<>();
    }
} 