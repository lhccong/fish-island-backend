package com.cong.fishisland.utils;

import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cong.fishisland.model.dto.pay.XunhuPayNotifyRequest;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.*;

/**
 * 虎皮椒支付工具类
 * <p>
 * 文档：https://www.xunhupay.com/doc/api/pay.html
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Slf4j
public class XunhuPayUtils {

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private XunhuPayUtils() {
    }

    /**
     * 生成签名（HASH）
     * <p>
     * 规则：将非空参数按参数名 ASCII 字典序排序，拼接成 key=value&key=value 格式，
     * 末尾直接拼接 appsecret（无连接符），对整体做 MD5（32位小写）。
     * hash 字段本身不参与签名。
     *
     * @param params     请求参数 Map
     * @param appsecret  应用密钥
     * @return MD5 签名字符串
     */
    public static String createSign(Map<String, Object> params, String appsecret) {
        // 过滤空值和 hash 字段，按 ASCII 字典序排序
        String[] sortedKeys = params.keySet().stream()
                .filter(k -> !"hash".equals(k) && params.get(k) != null && !"".equals(String.valueOf(params.get(k))))
                .sorted()
                .toArray(String[]::new);

        StringBuilder sb = new StringBuilder();
        for (String key : sortedKeys) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(key).append("=").append(params.get(key));
        }
        // 末尾直接拼接密钥，无连接符
        sb.append(appsecret);
        return SecureUtil.md5(sb.toString());
    }

    /**
     * 验证回调签名（Map 版本）
     *
     * @param params    回调参数 Map（key-value 均为 String）
     * @param appsecret 应用密钥
     * @return 签名是否合法
     */
    public static boolean verifySign(Map<String, String> params, String appsecret) {
        String receivedHash = params.get("hash");
        if (receivedHash == null || receivedHash.isEmpty()) {
            return false;
        }
        // 将 String Map 转为 Object Map 复用 createSign
        Map<String, Object> objectMap = new HashMap<>(params);
        String expectedHash = createSign(objectMap, appsecret);
        return expectedHash.equalsIgnoreCase(receivedHash);
    }

    /**
     * 验证回调签名（实体类版本）
     * <p>
     * 将实体类转为 Map 后复用签名逻辑，hash 字段自动排除。
     *
     * @param notify    回调参数实体
     * @param appsecret 应用密钥
     * @return 签名是否合法
     */
    public static boolean verifySign(XunhuPayNotifyRequest notify, String appsecret) {
        if (notify == null || notify.getHash() == null || notify.getHash().isEmpty()) {
            return false;
        }
        Map<String, Object> params = new java.util.LinkedHashMap<>();
        if (notify.getTrade_order_id() != null) {
            params.put("trade_order_id", notify.getTrade_order_id());
        }
        if (notify.getTotal_fee() != null) {
            params.put("total_fee", notify.getTotal_fee());
        }
        if (notify.getTransaction_id() != null) {
            params.put("transaction_id", notify.getTransaction_id());
        }
        if (notify.getOpen_order_id() != null) {
            params.put("open_order_id", notify.getOpen_order_id());
        }
        if (notify.getOrder_title() != null) {
            params.put("order_title", notify.getOrder_title());
        }
        if (notify.getStatus() != null) {
            params.put("status", notify.getStatus());
        }
        if (notify.getAppid() != null) {
            params.put("appid", notify.getAppid());
        }
        if (notify.getTime() != null) {
            params.put("time", notify.getTime());
        }
        if (notify.getNonce_str() != null) {
            params.put("nonce_str", notify.getNonce_str());
        }
        if (notify.getPlugins() != null) {
            params.put("plugins", notify.getPlugins());
        }
        if (notify.getAttach() != null) {
            params.put("attach", notify.getAttach());
        }
        // hash 字段不参与签名，不放入 map
        String expectedHash = createSign(params, appsecret);
        return expectedHash.equalsIgnoreCase(notify.getHash());
    }

    /**
     * 发起支付请求
     *
     * @param params     请求参数（已包含 hash）
     * @param gatewayUrl 支付网关地址
     * @return 虎皮椒返回的 JSON 对象，失败时返回 null
     */
    public static JSONObject doPayRequest(Map<String, Object> params, String gatewayUrl) {
        String body = JSON.toJSONString(params);
        RequestBody requestBody = RequestBody.create(body, JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(gatewayUrl)
                .post(requestBody)
                .build();
        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.error("[XunhuPay] HTTP 请求失败，状态码：{}", response.code());
                return null;
            }
            String responseBody = response.body().string();
            log.info("[XunhuPay] 支付接口响应：{}", responseBody);
            return JSONObject.parseObject(responseBody);
        } catch (IOException e) {
            log.error("[XunhuPay] 支付接口请求异常", e);
            return null;
        }
    }

    /**
     * 获取当前秒级时间戳
     *
     * @return 秒级时间戳
     */
    public static long currentSecondTimestamp() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * 生成指定长度的随机数字字符串
     *
     * @param length 长度
     * @return 随机数字字符串
     */
    public static String randomNonceStr(int length) {
        String chars = "0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
