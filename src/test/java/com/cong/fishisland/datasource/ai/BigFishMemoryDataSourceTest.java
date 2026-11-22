package com.cong.fishisland.datasource.ai;

import cn.hutool.core.io.IoUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class BigFishMemoryDataSourceTest {
    private final AtomicReference<JSONObject> write = new AtomicReference<>();
    private HttpServer server;
    private String url;
    private BigFishMemoryDataSource memory;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        url = "http://127.0.0.1:" + server.getAddress().getPort();
        memory = new BigFishMemoryDataSource(url, "test-secret");
        server.createContext("/auto_memory", exchange -> {
            write.set(request(exchange));
            respond(exchange, 200, Collections.singletonMap("success", true));
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void onlyReturnsDailyPassagesWithTheSameRoomAndUserScope() {
        List<String> messages = Arrays.asList("我喜欢帆船", "在练习画帆船");
        memory.remember(-1L, 7L, "synthetic-batch", messages);
        JSONObject payload = write.get();
        assertEquals("synthetic-batch", payload.getString("request_id"));
        assertEquals("user", payload.getJSONArray("messages").getJSONObject(0).getString("role"));
        assertEquals(messages.get(1), payload.getJSONArray("messages").getJSONObject(1).getString("content"));
        String owner = payload.getString("session_id");
        assertTrue(owner.matches("fish-[0-9a-f]{64}"));

        AtomicReference<JSONObject> search = new AtomicReference<>();
        server.createContext("/big_fish_search", exchange -> {
            search.set(request(exchange));
            List<JSONObject> results = Arrays.asList(
                    passage(null, "daily/2026-01-01/no-owner.md", "没有归属"),
                    passage(owner + "-other", "daily/2026-01-01/other.md", "其他会话"),
                    passage(owner, "digest/mixed.md", "跨会话汇总"),
                    passage(owner, "daily/2026-01-01/owner.md", "喜欢帆船"));
            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("answer", "汇总文本不供召回");
            response.put("metadata", Collections.singletonMap("results", results));
            respond(exchange, 200, response);
        });

        assertEquals(Collections.singletonList("喜欢帆船"), memory.recall(-1L, 7L, "帆船"));
        assertEquals(owner, search.get().getJSONObject("search_filter").getString("session_id"));
        assertEquals("帆船", search.get().getString("query"));
        assertTrue(memory.recall(-1L, 8L, "帆船").isEmpty());
        assertTrue(memory.recall(2L, 7L, "帆船").isEmpty());
    }

    @Test
    void missingConfigurationAndFailedRecallReturnEmptyContext() {
        server.createContext("/big_fish_search", exchange -> respond(exchange, 503, Collections.emptyMap()));
        assertTrue(memory.recall(-1L, 7L, "帆船").isEmpty());
        BigFishMemoryDataSource disabled = new BigFishMemoryDataSource(url, "");
        assertFalse(disabled.isConfigured());
        assertTrue(disabled.recall(-1L, 7L, "帆船").isEmpty());
    }

    private JSONObject passage(String sessionId, String path, String text) {
        JSONObject result = new JSONObject();
        result.put("metadata", Collections.singletonMap("session_id", sessionId));
        result.put("path", path);
        result.put("text", text);
        return result;
    }

    private JSONObject request(HttpExchange exchange) {
        return JSON.parseObject(IoUtil.read(exchange.getRequestBody(), StandardCharsets.UTF_8));
    }

    private void respond(HttpExchange exchange, int status, Object response) throws IOException {
        byte[] bytes = JSON.toJSONString(response).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
