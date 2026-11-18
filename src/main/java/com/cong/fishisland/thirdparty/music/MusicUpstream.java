package com.cong.fishisland.thirdparty.music;

import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/** Meting adapter. No chat state, socket, database or platform cookies. */
@Service
public class MusicUpstream {
    private final MusicProperties properties;
    private final ObjectMapper mapper;
    private final Semaphore requests = new Semaphore(6);
    private final Cache<String, JsonNode> searches = Caffeine.newBuilder()
            .maximumSize(200).expireAfterWrite(30, TimeUnit.SECONDS).build();

    public MusicUpstream(MusicProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
    }

    public JsonNode search(String source, String keyword, int page) {
        validateSource(source);
        if (keyword == null || keyword.trim().isEmpty() || keyword.length() > 80 || page < 1 || page > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索关键词或页数无效");
        }
        String word = keyword.trim();
        return searches.get(source + ":" + page + ":" + word, key -> {
            try {
                JsonNode raw = mapper.readTree(fetch(source, "search", word, "&page=" + page + "&limit=20"));
                return normalizeSongs(raw);
            } catch (BusinessException error) {
                throw error;
            } catch (Exception ignored) {
                throw unavailable();
            }
        });
    }

    public JsonNode recommendations(String source) {
        validateSource(source);
        try {
            JsonNode raw = mapper.readTree(fetch(source, "fm", "", ""));
            for (int depth = 0; depth < 4 && raw != null && raw.isObject(); depth++) {
                JsonNode next = null;
                for (String field : new String[]{"data", "songs", "results", "list"}) {
                    if (raw.has(field)) { next = raw.get(field); break; }
                }
                if (next == null) break;
                raw = next;
            }
            if (raw != null && raw.isObject() && !first(raw, "name", "title").isEmpty()) {
                raw = mapper.createArrayNode().add(raw);
            }
            return normalizeSongs(raw);
        } catch (BusinessException error) {
            throw error;
        } catch (Exception ignored) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "今日推荐暂不可用，请稍后重试或搜索歌曲");
        }
    }

    private JsonNode normalizeSongs(JsonNode raw) {
        if (raw == null || !raw.isArray()) throw unavailable();
        ArrayNode songs = mapper.createArrayNode();
        Set<String> seen = new HashSet<>();
        for (JsonNode row : raw) {
            String id = first(row, "id", "songId", "mid");
            if (id.isEmpty()) id = idFromUrl(row.path("url").asText());
            String name = first(row, "name", "title");
            if (id.isEmpty() || id.length() > 200 || name.isEmpty() || !seen.add(id)) continue;
            ObjectNode song = mapper.createObjectNode();
            song.put("id", id);
            song.put("name", truncate(name));
            song.put("artist", artist(row));
            songs.add(song);
            if (songs.size() == 20) break;
        }
        return songs;
    }

    public String resolve(String source, String id) {
        validateSource(source);
        if (id == null || id.trim().isEmpty() || id.length() > 200) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "歌曲编号无效");
        }
        try {
            String body = fetch(source, "url", id, "&quality=standard").trim();
            if (body.startsWith("{") || body.startsWith("\"")) {
                JsonNode raw = mapper.readTree(body);
                body = raw.isTextual() ? raw.asText() : raw.path("url").asText();
            }
            if (body.startsWith("@")) body = body.substring(1).trim();
            return publicMediaUrl(body);
        } catch (BusinessException error) {
            throw error;
        } catch (Exception ignored) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "这首歌曲暂无可用音源，请换一首");
        }
    }

    private String fetch(String source, String type, String id, String extra) {
        String token = properties.getToken().trim();
        if (properties.getApiUrl().trim().isEmpty() || token.isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "音乐服务尚未配置，请在后端环境 yml 或环境变量中配置地址和密钥");
        }
        if (!requests.tryAcquire()) throw new BusinessException(ErrorCode.REPEAT_SUBMIT_ERROR, "音乐服务繁忙，请稍后重试");
        HttpURLConnection connection = null;
        try {
            URI base = URI.create(properties.getApiUrl().replaceAll("/+$", ""));
            if (!"https".equalsIgnoreCase(base.getScheme()) || base.getHost() == null || base.getUserInfo() != null
                    || base.getRawQuery() != null || base.getFragment() != null) throw unavailable();
            String query = "/api?server=" + source + "&type=" + type + "&id="
                    + URLEncoder.encode(id, "UTF-8") + extra;
            connection = openConnection(URI.create(base + query));
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(8000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Accept", "application/json, text/plain");
            int status = connection.getResponseCode();
            if (status == 401 || status == 403) throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "音乐接口拒绝请求，请检查后端密钥或歌曲权限");
            if (status >= 300 && status < 400 && "url".equals(type)) return publicMediaUrl(connection.getHeaderField("Location"));
            if (status != 200) throw unavailable();
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int length;
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                while ((length = input.read(buffer)) != -1) {
                    if (output.size() + length > 524288 || System.nanoTime() > deadline) throw unavailable();
                    output.write(buffer, 0, length);
                }
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            }
        } catch (BusinessException error) {
            throw error;
        } catch (Exception ignored) {
            // Never log upstream response bodies, signed URLs or configuration values.
            throw unavailable();
        } finally {
            if (connection != null) connection.disconnect();
            requests.release();
        }
    }

    HttpURLConnection openConnection(URI uri) throws IOException {
        return (HttpURLConnection) uri.toURL().openConnection();
    }

    static String publicMediaUrl(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            String query = uri.getRawQuery() == null ? "" : uri.getRawQuery();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || !host.contains(".")
                    || uri.getUserInfo() != null || host.endsWith(".local")
                    || host.matches("(?i)^(localhost|127\\..*|10\\..*|192\\.168\\..*|169\\.254\\..*|172\\.(1[6-9]|2[0-9]|3[01])\\..*)$")
                    || query.matches("(?i).*(^|&)(auth=|type=url).*" )
                    || uri.getPath().contains("/song/media/outer/url")) throw unavailable();
            return uri.toASCIIString();
        } catch (Exception ignored) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "这首歌曲暂无可用音源，请换一首");
        }
    }

    private static void validateSource(String source) {
        if (!"netease".equals(source) && !"tencent".equals(source)) throw new BusinessException(ErrorCode.PARAMS_ERROR, "音乐来源无效");
    }

    private static String first(JsonNode row, String... names) {
        for (String name : names) {
            String value = row.path(name).asText("").trim();
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private static String idFromUrl(String value) {
        if (value != null && value.matches("[a-zA-Z0-9_-]{1,200}")) return value;
        try {
            String query = URI.create(value).getRawQuery();
            if (query != null) for (String part : query.split("&")) {
                if (part.startsWith("id=")) return java.net.URLDecoder.decode(part.substring(3), "UTF-8");
            }
        } catch (Exception ignored) { /* malformed row is skipped */ }
        return "";
    }

    private static String artist(JsonNode row) {
        JsonNode value = row.has("artist") ? row.get("artist") : row.path("author");
        if (!value.isArray()) return truncate(value.asText("未知歌手"));
        StringBuilder result = new StringBuilder();
        for (JsonNode item : value) {
            if (result.length() > 200) break;
            if (result.length() > 0) result.append(" / ");
            result.append(item.isTextual() ? item.asText() : item.path("name").asText());
        }
        return truncate(result.length() == 0 ? "未知歌手" : result.toString());
    }

    private static String truncate(String value) { return value.substring(0, Math.min(200, value.length())); }
    private static BusinessException unavailable() { return new BusinessException(ErrorCode.OPERATION_ERROR, "音乐服务暂不可用，请稍后重试"); }
}
