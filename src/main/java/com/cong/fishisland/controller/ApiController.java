package com.cong.fishisland.controller;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * API接口控制器
 *
 * @author cong
 */
@RestController
@RequestMapping("/")
@Slf4j
@Api(tags = "API接口")
public class ApiController {

    private final OkHttpClient httpClient;
    private final Random random = new Random();

    // BV转换所需的常量
    private static final String TABLE = "fZodR9XQDSUm21yCkr6zBqiveYah8bt4xsWpHnJE7jL5VG3guMTKNPAwcF";
    private static final int[] S = {11, 10, 3, 8, 4, 6};
    private static final long XOR = 177451812;
    private static final long ADD = 8728348608L;

    // 年份范围配置
    private static final Map<Integer, int[]> YEAR_RANGES = new HashMap<>();
    
    static {
        YEAR_RANGES.put(2013, new int[]{1, 1000000});
        YEAR_RANGES.put(2014, new int[]{1000001, 2000000});
        YEAR_RANGES.put(2015, new int[]{2000001, 4000000});
        YEAR_RANGES.put(2016, new int[]{4000001, 8000000});
        YEAR_RANGES.put(2017, new int[]{8000001, 16000000});
        YEAR_RANGES.put(2018, new int[]{16000001, 35000000});
        YEAR_RANGES.put(2019, new int[]{35000001, 60000000});
        YEAR_RANGES.put(2020, new int[]{60000001, 85000000});
        YEAR_RANGES.put(2021, new int[]{85000001, 500000000});
        YEAR_RANGES.put(2022, new int[]{500000001, 700000000});
        YEAR_RANGES.put(2023, new int[]{700000001, 835000000});
        YEAR_RANGES.put(2024, new int[]{835000001, 950000000});
    }

    /**
     * 哔哩哔哩 音频代理API（处理跨域与Referer/Range防盗链）
     * 示例：/proxy/audio?url=ENCODED_M4S_URL&referer=https%3A%2F%2Fwww.bilibili.com%2F
     */
    @GetMapping("/proxy/audio")
    @ApiOperation(value = "哔哩哔哩音频代理API（支持Range）")
    public void proxyAudio(HttpServletRequest request, HttpServletResponse response) {
        String audioUrl = request.getParameter("url");
        String referer = request.getParameter("referer");
        if (referer == null || referer.isEmpty()) {
            referer = "https://www.bilibili.com/";
        }
        if (audioUrl == null || audioUrl.isEmpty()) {
            response.setStatus(400);
            return;
        }

        try {
            Request.Builder builder = new Request.Builder()
                    .url(audioUrl)
                    .addHeader("Referer", referer)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36");
            String range = request.getHeader("Range");
            if (range != null) {
                builder.addHeader("Range", range);
            }
            Request outbound = builder.build();

            try (Response upstream = httpClient.newCall(outbound).execute()) {
                if (upstream.body() == null) {
                    response.setStatus(502);
                    return;
                }
                // 复制上游响应头（过滤部分头）
                for (String name : upstream.headers().names()) {
                    String lower = name.toLowerCase();
                    if ("content-length".equals(lower) || "transfer-encoding".equals(lower)) continue;
                    response.setHeader(name, upstream.header(name));
                }
                // CORS 友好
                response.setHeader("Access-Control-Allow-Origin", "*");
                response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Range");

                response.setStatus(upstream.code());

                try (InputStream in = upstream.body().byteStream();
                     java.io.OutputStream out = response.getOutputStream()) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                    }
                    out.flush();
                }
            }
        } catch (IOException e) {
            log.error("音频代理请求失败: {}", e.getMessage());
            try {
                response.setStatus(500);
            } catch (Exception ignore) {}
        }
    }

    /**
     * 哔哩哔哩 图片代理API（解决 B 站图片 403 / 跨域）
     * 示例：/proxy/image?url=ENCODED_IMAGE_URL
     */
    @GetMapping("/proxy/image")
    @ApiOperation(value = "图片代理API（支持缓存与CORS）")
    public void proxyImage(HttpServletRequest request, HttpServletResponse response) {
        String imageUrl = request.getParameter("url");
        if (imageUrl == null || imageUrl.isEmpty()) {
            response.setStatus(400);
            return;
        }

        // 针对 B 站域名使用特定 Referer
        String referer = (imageUrl.contains("hdslb.com") || imageUrl.contains("bili"))
                ? "https://www.bilibili.com/"
                : "https://start-page.live";

        try {
            Request outbound = new Request.Builder()
                    .url(imageUrl)
                    .addHeader("Referer", referer)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36")
                    .addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .build();

            try (Response upstream = httpClient.newCall(outbound).execute()) {
                if (upstream.body() == null) {
                    response.setStatus(502);
                    return;
                }

                String contentType = upstream.header("Content-Type");
                if (contentType == null || contentType.isEmpty()) {
                    contentType = "image/jpeg";
                }
                response.setHeader("Content-Type", contentType);
                // 缓存 1 天
                response.setHeader("Cache-Control", "public, max-age=86400");
                // CORS 友好
                response.setHeader("Access-Control-Allow-Origin", "*");

                response.setStatus(upstream.code());

                try (InputStream in = upstream.body().byteStream();
                     java.io.OutputStream out = response.getOutputStream()) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                    }
                    out.flush();
                }
            }
        } catch (IOException e) {
            log.error("图片代理请求失败: {}", e.getMessage());
            // 回退到占位图
            try {
                String placeholder = "https://i0.hdslb.com/bfs/archive/71dd6f2cdc5c8ab8607ce51b9dc3f5fa32c74824.jpg";
                Request fallback = new Request.Builder()
                        .url(placeholder)
                        .addHeader("Referer", "https://www.bilibili.com/")
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build();
                try (Response up = httpClient.newCall(fallback).execute()) {
                    if (up.body() != null) {
                        response.setHeader("Content-Type", up.header("Content-Type", "image/jpeg"));
                        response.setHeader("Cache-Control", "public, max-age=86400");
                        response.setHeader("Access-Control-Allow-Origin", "*");
                        response.setStatus(200);
                        try (InputStream in = up.body().byteStream();
                             java.io.OutputStream out = response.getOutputStream()) {
                            byte[] buf = new byte[8192];
                            int n;
                            while ((n = in.read(buf)) != -1) {
                                out.write(buf, 0, n);
                            }
                            out.flush();
                        }
                        return;
                    }
                }
            } catch (Exception ignore) {}
            try {
                response.setStatus(500);
            } catch (Exception ignore) {}
        }
    }

    public ApiController() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 随机小姐姐API
     *
     * @return 随机小姐姐数据
     */
    @GetMapping("/miss")
    @ApiOperation(value = "随机小姐姐API")
    public BaseResponse<Object> getRandomMiss() {
        try {
            Request request = new Request.Builder()
                    .url("https://v2.xxapi.cn/api/meinv")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .addHeader("Referer", "https://v2.xxapi.cn/")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    // 解析JSON响应
                    com.alibaba.fastjson.JSONObject jsonResponse = com.alibaba.fastjson.JSON.parseObject(responseBody);
                    return ResultUtils.success(jsonResponse);
                } else {
                    log.error("随机小姐姐API请求失败: HTTP {}", response.code());
                    return ResultUtils.error(500, "服务器内部错误");
                }
            }
        } catch (IOException e) {
            log.error("随机小姐姐API请求失败: {}", e.getMessage());
            return ResultUtils.error(500, "服务器内部错误");
        }
    }

    /**
     * 随机哔哩哔哩视频API
     *
     * @return 随机B站视频信息
     */
    @GetMapping("/bilibili/random")
    @ApiOperation(value = "随机哔哩哔哩视频API")
    public BaseResponse<Map<String, String>> getRandomBilibiliVideo() {
        try {
            String bvid = generateRandomBV();
            int attempts = 0;
            final int maxAttempts = 5;

            while (!verifyBV(bvid) && attempts < maxAttempts) {
                bvid = generateRandomBV();
                attempts++;
            }

            if (attempts >= maxAttempts) {
                return ResultUtils.error(500, "无法找到有效的随机视频，请重试");
            }

            Map<String, String> result = new HashMap<>();
            result.put("bvid", bvid);
            result.put("url", "https://www.bilibili.com/video/" + bvid);

            return ResultUtils.success(result);
        } catch (Exception e) {
            log.error("随机B站视频API请求失败: {}", e.getMessage());
            return ResultUtils.error(500, "服务器内部错误");
        }
    }

    /**
     * 生成随机BV号
     *
     * @return 随机BV号
     */
    private String generateRandomBV() {
        Integer[] years = YEAR_RANGES.keySet().toArray(new Integer[0]);
        int randomYear = years[random.nextInt(years.length)];
        int[] range = YEAR_RANGES.get(randomYear);
        long avid = random.nextInt(range[1] - range[0]) + range[0];
        return enc(avid);
    }

    /**
     * AV号转BV号
     *
     * @param x AV号
     * @return BV号
     */
    private String enc(long x) {
        x = (x ^ XOR) + ADD;
        char[] r = "BV1  4 1 7  ".toCharArray();
        for (int i = 0; i < 6; i++) {
            r[S[i]] = TABLE.charAt((int) (Math.floor(x / Math.pow(58, i)) % 58));
        }
        return new String(r);
    }

    /**
     * 验证BV号是否存在
     *
     * @param bvid BV号
     * @return 是否存在
     */
    private boolean verifyBV(String bvid) {
        try {
            Request request = new Request.Builder()
                    .url("https://api.bilibili.com/x/web-interface/view?bvid=" + bvid)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .addHeader("Referer", "https://www.bilibili.com")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    // 简单检查响应是否包含成功标识
                    return responseBody.contains("\"code\":0");
                }
            }
        } catch (IOException e) {
            log.debug("验证BV号失败: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 歌词适配·网易云解析接口
     */
    @RequestMapping(value = "/proxy/wy/**", method = {RequestMethod.GET, RequestMethod.POST})
    @ApiOperation(value = "歌词适配·网易云解析接口")
    public BaseResponse<Object> proxyWy(HttpServletRequest request) {
        try {
            String uri = request.getRequestURI();
            String prefix = "/proxy/wy/";
            int idx = uri.indexOf(prefix);
            String rest = idx >= 0 ? uri.substring(idx + prefix.length()) : "";

            StringBuilder target = new StringBuilder("https://www.gecishipei.com/").append(rest);
            String query = request.getQueryString();
            if (query != null && !query.isEmpty()) {
                target.append('?').append(query);
            }

            Request.Builder builder = new Request.Builder()
                    .url(target.toString())
                    .addHeader("origin", "https://www.gecishipei.com")
                    .addHeader("referer", "https://www.gecishipei.com/wy/")
                    .addHeader("x-requested-with", "XMLHttpRequest")
                    .addHeader("accept", "application/json, text/javascript, */*; q=0.01")
                    .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36");

            // 透传 POST 请求体（必要时强制以 x-www-form-urlencoded 转发）
            if ("POST".equalsIgnoreCase(request.getMethod())) {
                String contentType = request.getContentType();
                boolean isTextPlain = contentType != null && contentType.toLowerCase().startsWith("text/plain");
                // 目标 /wy/ 接口需要表单；若前端误传 text/plain，这里统一转为表单
                if (contentType == null || isTextPlain) {
                    contentType = "application/x-www-form-urlencoded";
                }
                MediaType mediaType = MediaType.parse(contentType);
                byte[] bytes;
                try (InputStream in = request.getInputStream(); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    byte[] data = new byte[4096];
                    int n;
                    while ((n = in.read(data)) != -1) {
                        buffer.write(data, 0, n);
                    }
                    bytes = buffer.toByteArray();
                }
                RequestBody reqBody = RequestBody.create(mediaType, bytes);
                builder = builder.post(reqBody);
                builder = builder.removeHeader("content-type").addHeader("content-type", contentType);
            }

            Request outbound = builder.build();

            try (Response resp = httpClient.newCall(outbound).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    log.error("中转请求失败: HTTP {} -> {}", resp.code(), target);
                    return ResultUtils.error(500, "目标服务异常");
                }
                String body = resp.body().string();
                try {
                    // 优先尝试解析为 JSON 并透传
                    com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(body);
                    return ResultUtils.success(json);
                } catch (Exception ignore) {
                    // 返回非 JSON（如纯文本）时直接透传字符串
                    Map<String, Object> wrap = new HashMap<>();
                    wrap.put("data", body);
                    return ResultUtils.success(wrap);
                }
            }
        } catch (IOException e) {
            log.error("中转调用异常: {}", e.getMessage());
            return ResultUtils.error(500, "服务器内部错误");
        }
    }

    /**
     * QQ 音乐 - 搜索（后端代请求，避免前端 400/CORS；自动补齐必要头）
     * 示例：/qq/search?q=关键词&p=1&n=20
     */
    @GetMapping("/qq/search")
    @ApiOperation(value = "QQ音乐搜索")
    public BaseResponse<Object> qqSearch(HttpServletRequest request) {
        try {
            String q = request.getParameter("q");
            String p = request.getParameter("p");
            String n = request.getParameter("n");
            if (q == null || q.trim().isEmpty()) {
                return ResultUtils.error(400, "请提供搜索关键词 q");
            }
            if (p == null || p.isEmpty()) p = "1";
            if (n == null || n.isEmpty()) n = "20";

            String url = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp?format=json"
                    + "&w=" + java.net.URLEncoder.encode(q, java.nio.charset.StandardCharsets.UTF_8.name())
                    + "&p=" + p
                    + "&n=" + n;

            Request outbound = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36")
                    .addHeader("Referer", "https://y.qq.com/portal/player.html")
                    .addHeader("Origin", "https://y.qq.com")
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .build();

            try (Response resp = httpClient.newCall(outbound).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    return ResultUtils.error(500, "QQ搜索失败");
                }
                String body = resp.body().string();
                com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(body);
                return ResultUtils.success(json);
            }
        } catch (Exception e) {
            log.error("QQ搜索异常: {}", e.getMessage());
            return ResultUtils.error(500, "服务器内部错误");
        }
    }

    /**
     * QQ 音乐 - 歌词
     * 支持：1) songmid 参数，走 fcg_query_lyric_new；2) musicid（数字ID），走 lyric_download.fcg，提取 CDATA
     * 示例：/qq/lyric?songmid=003aAYrm3GE0Ac
     */
    @GetMapping("/qq/lyric")
    @ApiOperation(value = "QQ音乐歌词")
    public BaseResponse<Object> qqLyric(HttpServletRequest request) {
        String songmid = request.getParameter("songmid");
        String musicid = request.getParameter("musicid");
        if ((songmid == null || songmid.isEmpty()) && (musicid == null || musicid.isEmpty())) {
            return ResultUtils.error(400, "请提供 songmid 或 musicid");
        }
        try {
            if (songmid != null && !songmid.isEmpty()) {
                String url = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?songmid="
                        + java.net.URLEncoder.encode(songmid, java.nio.charset.StandardCharsets.UTF_8.name())
                        + "&format=json&nobase64=1";
                Request outbound = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36")
                        .addHeader("Referer", "https://y.qq.com/portal/player.html")
                        .addHeader("Origin", "https://y.qq.com")
                        .addHeader("Accept", "application/json, text/plain, */*")
                        .build();
                try (Response resp = httpClient.newCall(outbound).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        return ResultUtils.error(500, "QQ歌词获取失败");
                    }
                    String body = resp.body().string();
                    com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(body);
                    // 兼容返回结构：{ lyric: "...", trans: "..." }
                    Map<String, Object> result = new HashMap<>();
                    if (json != null) {
                        if (json.containsKey("lyric")) result.put("lyric", json.getString("lyric"));
                        if (json.containsKey("trans")) result.put("trans", json.getString("trans"));
                        result.put("raw", json);
                    }
                    return ResultUtils.success(result);
                }
            } else {
                String url = "https://c.y.qq.com/qqmusic/fcgi-bin/lyric_download.fcg?version=15&lrctype=4&musicid="
                        + java.net.URLEncoder.encode(musicid, java.nio.charset.StandardCharsets.UTF_8.name());
                Request outbound = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36")
                        .addHeader("Referer", "https://y.qq.com/portal/player.html")
                        .addHeader("Origin", "https://y.qq.com")
                        .addHeader("Accept", "text/plain, */*")
                        .build();
                try (Response resp = httpClient.newCall(outbound).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        return ResultUtils.error(500, "QQ歌词获取失败");
                    }
                    String text = resp.body().string();
                    // 提取 CDATA
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("CDATA\\[(.+?)]]").matcher(text);
                    String original = null;
                    String translate = null;
                    if (m.find()) original = m.group(1);
                    if (m.find()) translate = m.group(1);
                    Map<String, Object> result = new HashMap<>();
                    if (original != null) result.put("lyric", original);
                    if (translate != null) result.put("trans", translate);
                    result.put("raw", text);
                    return ResultUtils.success(result);
                }
            }
        } catch (Exception e) {
            log.error("QQ歌词异常: {}", e.getMessage());
            return ResultUtils.error(500, "服务器内部错误");
        }
    }

    /**
     * QQ 音乐 - 获取播放直链（通过 u.y.qq.com 获取 vkey/purl）
     * 示例：/qq/play?songmid=003Ue6Ia32q4gl&albumMid=002Gbc9W0oOAS3
     */
    @GetMapping("/qq/play")
    @ApiOperation(value = "QQ音乐播放直链")
    public BaseResponse<Object> qqPlay(HttpServletRequest request) {
        String songmid = request.getParameter("songmid");
        String albumMid = request.getParameter("albumMid");
        if (songmid == null || songmid.trim().isEmpty()) {
            return ResultUtils.error(400, "请提供 songmid");
        }
        try {
            String guid = String.valueOf(10000000L + Math.abs(random.nextLong()) % 90000000L);
            String dataJson = "{\"req_0\":{\"module\":\"vkey.GetVkeyServer\",\"method\":\"CgiGetVkey\",\"param\":{\"guid\":\"" + guid + "\",\"songmid\":[\"" + songmid + "\"],\"songtype\":[0],\"uin\":\"0\",\"loginflag\":1,\"platform\":\"20\"}},\"comm\":{\"uin\":\"0\",\"format\":\"json\",\"ct\":24,\"cv\":0}}";
            String url = "https://u.y.qq.com/cgi-bin/musicu.fcg?format=json&data=" + java.net.URLEncoder.encode(dataJson, java.nio.charset.StandardCharsets.UTF_8.name());

            Request outbound = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36")
                    .addHeader("Referer", "https://y.qq.com/portal/player.html")
                    .addHeader("Origin", "https://y.qq.com")
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .build();

            try (Response resp = httpClient.newCall(outbound).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    return ResultUtils.error(500, "获取播放地址失败");
                }
                String body = resp.body().string();
                com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(body);
                com.alibaba.fastjson.JSONObject req0 = json.getJSONObject("req_0");
                String purl = null;
                String sip = null;
                String filename = null;
                if (req0 != null && req0.getJSONObject("data") != null) {
                    com.alibaba.fastjson.JSONObject data = req0.getJSONObject("data");
                    if (data.getJSONArray("midurlinfo") != null && !data.getJSONArray("midurlinfo").isEmpty()) {
                        com.alibaba.fastjson.JSONObject info = data.getJSONArray("midurlinfo").getJSONObject(0);
                        purl = info.getString("purl");
                        filename = info.getString("filename");
                    }
                    if (data.getJSONArray("sip") != null && !data.getJSONArray("sip").isEmpty()) {
                        sip = data.getJSONArray("sip").getString(0);
                    }
                }

                String playUrl = null;
                if (purl != null && !purl.isEmpty() && sip != null) {
                    playUrl = sip + purl;
                } else if (filename != null && !filename.isEmpty() && sip != null) {
                    playUrl = sip + filename;
                }
                if (playUrl == null) {
                    return ResultUtils.error(404, "未获取到可播放链接");
                }

                Map<String, Object> result = new HashMap<>();
                result.put("audioUrl", playUrl);
                Map<String, String> headers = new HashMap<>();
                headers.put("Referer", "https://y.qq.com/portal/player.html");
                headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36");
                result.put("headers", headers);
                if (albumMid != null && !albumMid.isEmpty()) {
                    String cover = "https://y.gtimg.cn/music/photo_new/T002R300x300M000" + albumMid + ".jpg";
                    result.put("cover", cover);
                }
                return ResultUtils.success(result);
            }
        } catch (Exception e) {
            log.error("QQ播放直链异常: {}", e.getMessage());
            return ResultUtils.error(500, "服务器内部错误");
        }
    }

    /**
     * 哔哩哔哩 视频分P信息获取
     * 示例：/bilibili/pages?bvid=BVxxxxxx
     */
    @GetMapping("/bilibili/pages")
    @ApiOperation(value = "哩哔哩 视频分P信息获取API")
    public BaseResponse<Object> getBilibiliPages(HttpServletRequest request) {
        try {
            String bvid = request.getParameter("bvid");
            if (bvid == null || bvid.trim().isEmpty()) {
                return ResultUtils.error(400, "请提供视频BV号");
            }

            String url = "https://www.bilibili.com/video/" + bvid;
            Request outbound = new Request.Builder()
                    .url(url)
                    .addHeader("Referer", "https://www.bilibili.com/")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36")
                    .build();

            try (Response resp = httpClient.newCall(outbound).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    return ResultUtils.error(500, "无法获取视频页面");
                }
                String html = resp.body().string();
                com.alibaba.fastjson.JSONObject initial = parseInitialStateFromHtml(html);
                if (initial == null || !initial.containsKey("videoData")) {
                    return ResultUtils.error(404, "无法获取视频信息");
                }

                com.alibaba.fastjson.JSONObject videoData = initial.getJSONObject("videoData");

                List<Map<String, Object>> formattedPages = new ArrayList<>();
                if (videoData.containsKey("pages")) {
                    for (Object p : videoData.getJSONArray("pages")) {
                        com.alibaba.fastjson.JSONObject page = (com.alibaba.fastjson.JSONObject) p;
                        Map<String, Object> item = new HashMap<>();
                        int pageNum = page.getIntValue("page");
                        item.put("page", pageNum);
                        item.put("part", page.getString("part"));
                        item.put("duration", page.getIntValue("duration"));
                        item.put("url", "https://www.bilibili.com/video/" + bvid + "?p=" + pageNum);
                        formattedPages.add(item);
                    }
                }

                Map<String, Object> result = new HashMap<>();
                result.put("title", videoData.getString("title"));
                result.put("bvid", bvid);
                result.put("pic", videoData.getString("pic"));
                result.put("pages", formattedPages);
                result.put("owner", videoData.getJSONObject("owner") == null ? new HashMap<>() : videoData.getJSONObject("owner"));
                result.put("isMultiPart", formattedPages.size() > 1);
                result.put("totalPages", formattedPages.size());

                return ResultUtils.success(result);
            }
        } catch (IOException e) {
            log.error("获取B站视频分P信息失败: {}", e.getMessage());
            return ResultUtils.error(500, "服务器内部错误");
        }
    }

    /**
     * B站视频音频解析API
     * 示例：/bilibili/audio?url=https://www.bilibili.com/video/BVxxxxxx?p=1
     */
    @GetMapping("/bilibili/audio")
    @ApiOperation(value = "B站视频音频解析API")
    public BaseResponse<Object> getBilibiliAudio(HttpServletRequest request) {
        try {
            String videoUrl = request.getParameter("url");
            if (videoUrl == null || !videoUrl.contains("bilibili.com/video")) {
                return ResultUtils.error(400, "请提供有效的哔哩哔哩视频URL");
            }

            // 解析视频ID与分P
            String bvid = null;
            Integer pageNum = 1;
            try {
                Matcher mBV = Pattern.compile("/video/(BV\\w+)", Pattern.CASE_INSENSITIVE).matcher(videoUrl);
                Matcher mAV = Pattern.compile("/video/av(\\d+)", Pattern.CASE_INSENSITIVE).matcher(videoUrl);
                if (mBV.find()) {
                    bvid = mBV.group(1);
                } else if (mAV.find()) {
                    bvid = "av" + mAV.group(1);
                }
                Matcher mP = Pattern.compile("[?&]p=(\\d+)", Pattern.CASE_INSENSITIVE).matcher(videoUrl);
                if (mP.find()) {
                    pageNum = Integer.parseInt(mP.group(1));
                }
            } catch (Exception ignore) {}

            if (bvid == null) {
                return ResultUtils.error(400, "无法从URL中提取视频ID");
            }

            String pageUrl = "https://www.bilibili.com/video/" + bvid + (pageNum != null ? ("?p=" + pageNum) : "");
            Request outbound = new Request.Builder()
                    .url(pageUrl)
                    .addHeader("Referer", "https://www.bilibili.com/")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36")
                    .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .build();

            try (Response resp = httpClient.newCall(outbound).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    return ResultUtils.error(500, "无法获取视频页面");
                }
                String html = resp.body().string();

                // 解析音频信息
                com.alibaba.fastjson.JSONObject playInfo = parsePlayInfoFromHtml(html);
                String audioUrl = null;
                String deadline = null;
                if (playInfo != null && playInfo.containsKey("data")
                        && playInfo.getJSONObject("data").containsKey("dash")) {
                    com.alibaba.fastjson.JSONObject dash = playInfo.getJSONObject("data").getJSONObject("dash");
                    if (dash != null && dash.containsKey("audio") && dash.getJSONArray("audio").size() > 0) {
                        // 选择码率最高的音频
                        com.alibaba.fastjson.JSONArray audios = dash.getJSONArray("audio");
                        int bestIdx = 0;
                        long bestBandwidth = -1;
                        for (int i = 0; i < audios.size(); i++) {
                            com.alibaba.fastjson.JSONObject a = audios.getJSONObject(i);
                            long bw = a.getLongValue("bandwidth");
                            if (bw > bestBandwidth) {
                                bestBandwidth = bw;
                                bestIdx = i;
                            }
                        }
                        com.alibaba.fastjson.JSONObject best = audios.getJSONObject(bestIdx);
                        audioUrl = best.getString("baseUrl");

                        // 提取 deadline 参数
                        try {
                            java.net.URL u = new java.net.URL(audioUrl);
                            String q = u.getQuery();
                            if (q != null) {
                                for (String part : q.split("&")) {
                                    int eq = part.indexOf('=');
                                    if (eq > 0) {
                                        String k = part.substring(0, eq);
                                        String v = part.substring(eq + 1);
                                        if ("deadline".equalsIgnoreCase(k)) {
                                            deadline = java.net.URLDecoder.decode(v, java.nio.charset.StandardCharsets.UTF_8.name());
                                        }
                                    }
                                }
                            }
                        } catch (Exception ignore) {}
                    }
                }

                // 解析初始状态，拿到标题、封面、分P等
                com.alibaba.fastjson.JSONObject initial = parseInitialStateFromHtml(html);
                String title = null;
                String cover = null;
                List<Map<String, Object>> pages = new ArrayList<>();
                Map<String, Object> currentPage = null;
                Map<String, Object> owner = null;
                if (initial != null && initial.containsKey("videoData")) {
                    com.alibaba.fastjson.JSONObject videoData = initial.getJSONObject("videoData");
                    title = videoData.getString("title");
                    cover = videoData.getString("pic");
                    if (videoData.getJSONObject("owner") != null) {
                        owner = new HashMap<>();
                        com.alibaba.fastjson.JSONObject o = videoData.getJSONObject("owner");
                        owner.put("name", o.getString("name"));
                        owner.put("mid", o.getLongValue("mid"));
                        owner.put("face", o.getString("face"));
                    }

                    if (cover != null) {
                        cover = cover.replace("\\u002F", "/").replace("\u002F", "/");
                        if (!cover.startsWith("http")) {
                            if (cover.startsWith("//")) {
                                cover = "https:" + cover;
                            } else {
                                cover = "https://" + cover;
                            }
                        }
                    }

                    if (videoData.containsKey("pages")) {
                        for (Object p : videoData.getJSONArray("pages")) {
                            com.alibaba.fastjson.JSONObject pg = (com.alibaba.fastjson.JSONObject) p;
                            Map<String, Object> item = new HashMap<>();
                            int pgNum = pg.getIntValue("page");
                            item.put("page", pgNum);
                            item.put("part", pg.getString("part"));
                            item.put("duration", pg.getIntValue("duration"));
                            item.put("url", "https://www.bilibili.com/video/" + bvid + "?p=" + pgNum);
                            pages.add(item);
                            if (pgNum == pageNum) {
                                currentPage = item;
                            }
                        }
                    }

                    // 多P时用分P标题
                    if (currentPage != null && currentPage.get("part") != null) {
                        String albumTitle = title;
                        String partTitle = String.valueOf(currentPage.get("part"));
                        if (partTitle != null && !partTitle.isEmpty()) {
                            title = partTitle;
                            if (partTitle.matches("^(第\\s*\\d+\\s*[集期话]|\\d+)$")) {
                                title = title + " - " + albumTitle;
                            }
                        }
                    }
                }

                if (audioUrl == null) {
                    return ResultUtils.error(404, "无法从视频中提取音频链接");
                }

                Map<String, Object> result = new HashMap<>();
                result.put("title", title);
                result.put("audioUrl", audioUrl);
                result.put("cover", cover);
                result.put("currentPage", currentPage);
                result.put("pages", pages);
                result.put("isMultiPart", pages.size() > 1);
                result.put("totalPages", pages.size());
                result.put("currentPageNumber", pageNum);
                result.put("videoId", bvid);
                result.put("deadline", deadline);
                if (owner != null) {
                    result.put("owner", owner);
                }
                Map<String, String> headers = new HashMap<>();
                headers.put("Referer", "https://www.bilibili.com/");
                headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36");
                result.put("headers", headers);

                return ResultUtils.success(result);
            }
        } catch (IOException e) {
            log.error("B站视频音频解析失败: {}", e.getMessage());
            return ResultUtils.error(500, "服务器内部错误");
        }
    }

    // 从 HTML 中解析 window.__INITIAL_STATE__ JSON（更稳健：优先 </script>，回退 ;(function())）
    private com.alibaba.fastjson.JSONObject parseInitialStateFromHtml(String html) {
        try {
            // 方案一：直到 </script>
            Pattern p1 = Pattern.compile("<script>window\\.__INITIAL_STATE__=(.*?)</script>", Pattern.DOTALL);
            Matcher m1 = p1.matcher(html);
            if (m1.find()) {
                String json = m1.group(1);
                return com.alibaba.fastjson.JSON.parseObject(json);
            }
            // 方案二：直到 ;(function())
            Pattern p2 = Pattern.compile("<script>window\\.__INITIAL_STATE__=(.*?);\\(function\\(\\)\\)", Pattern.DOTALL);
            Matcher m2 = p2.matcher(html);
            if (m2.find()) {
                String json = m2.group(1);
                return com.alibaba.fastjson.JSON.parseObject(json);
            }
        } catch (Exception ignore) {}
        return null;
    }

    /**
     * 抖音视频解析音频文件（通过第三方 aifooler 转发获取直链）
     * 前端传入任意抖音分享/视频页链接，返回可用于 <audio> 的播放地址（通常为 mp4/aac），以及封面/标题等
     * 示例：POST /douyin/parse  body: {"url":"https://www.douyin.com/..."}
     */
    @RequestMapping(value = "/douyin/parse", method = {RequestMethod.POST, RequestMethod.GET})
    @ApiOperation(value = "抖音视频解析音频文件")
    public BaseResponse<Object> parseDouyin(HttpServletRequest request) {
        try {
            // 兼容 GET 参数或 JSON Body
            String url = request.getParameter("url");
            if ((url == null || url.trim().isEmpty()) &&
                    ("POST".equalsIgnoreCase(request.getMethod()))) {
                try (InputStream in = request.getInputStream(); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    byte[] data = new byte[4096];
                    int n;
                    while ((n = in.read(data)) != -1) {
                        buffer.write(data, 0, n);
                    }
                    String body = new String(buffer.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
                    if (body != null && !body.isEmpty()) {
                        try {
                            com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(body);
                            url = json.getString("url");
                        } catch (Exception ignore) {}
                    }
                }
            }

            if (url == null || url.trim().isEmpty()) {
                return ResultUtils.error(400, "请提供抖音链接url");
            }

            // 请求第三方服务
            String api = "https://www.aifooler.com/api/parse-video-url";
            MediaType jsonType = MediaType.parse("application/json; charset=utf-8");
            String reqJson = "{\"url\":\"" + url.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
            RequestBody reqBody = RequestBody.create(jsonType, reqJson);

            Request outbound = new Request.Builder()
                    .url(api)
                    .post(reqBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Origin", "https://www.aifooler.com")
                    .addHeader("Referer", "https://www.aifooler.com/video-audio")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36")
                    .build();

            try (Response resp = httpClient.newCall(outbound).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    log.error("抖音解析失败: HTTP {}", resp.code());
                    return ResultUtils.error(500, "解析服务异常");
                }
                String body = resp.body().string();
                com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(body);
                if (json == null || !json.getBooleanValue("success")) {
                    return ResultUtils.error(500, json != null && json.containsKey("message") ? json.getString("message") : "解析失败");
                }
                com.alibaba.fastjson.JSONObject data = json.getJSONObject("data");
                if (data == null) {
                    return ResultUtils.error(404, "未获取到解析结果");
                }

                String title = data.getString("title");
                String videoUrl = data.getString("videoUrl");
                String coverUrl = data.getString("coverUrl");

                if (videoUrl == null || videoUrl.isEmpty()) {
                    return ResultUtils.error(404, "未获取到可播放链接");
                }

                Map<String, Object> result = new HashMap<>();
                result.put("title", title);
                // 直接作为音频播放（mp4/aac 一般可被 <audio> 播放）
                result.put("audioUrl", videoUrl);
                result.put("cover", coverUrl);
                // 播放建议带上 Referer（通过后端 /proxy/audio 使用）
                Map<String, String> headers = new HashMap<>();
                headers.put("Referer", "https://www.douyin.com/");
                headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36");
                result.put("headers", headers);

                return ResultUtils.success(result);
            }
        } catch (IOException e) {
            log.error("抖音视频解析失败: {}", e.getMessage());
            return ResultUtils.error(500, "服务器内部错误");
        }
    }

    // 从 HTML 中解析 window.__playinfo__ JSON
    private com.alibaba.fastjson.JSONObject parsePlayInfoFromHtml(String html) {
        try {
            Pattern p = Pattern.compile("<script>window\\.__playinfo__=(.*?)</script>", Pattern.DOTALL);
            Matcher m = p.matcher(html);
            if (m.find()) {
                String json = m.group(1);
                return com.alibaba.fastjson.JSON.parseObject(json);
            }
        } catch (Exception ignore) {}
        return null;
    }
}
