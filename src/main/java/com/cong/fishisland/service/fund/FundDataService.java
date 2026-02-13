package com.cong.fishisland.service.fund;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.time.LocalTime;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基金数据获取服务
 * 从多个数据源获取基金实时数据
 *
 * @author shing
 */
@Service
@Slf4j
public class FundDataService {

    // ==================== 常量定义 ====================
    
    // HTTP请求相关
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int DEFAULT_TIMEOUT = 2000;
    private static final int SHORT_TIMEOUT = 1000;
    private static final int LONG_TIMEOUT = 3000;
    private static final String CHARSET_GBK = "GBK";
    
    // 数据源标识
    private static final String SOURCE_SINA = "SINA_OFFICIAL";
    private static final String SOURCE_L2 = "LEVEL2_MARKET";
    private static final String SOURCE_EASTMONEY = "EASTMONEY_EST";
    
    // 状态标识
    private static final String STATUS_CLOSED = "closed";
    private static final String STATUS_TRADING = "trading";
    
    // API URL模板
    private static final String SINA_FUND_URL_TEMPLATE = "http://hq.sinajs.cn/list=f_%s";
    private static final String SINA_INDEX_URL_TEMPLATE = "http://hq.sinajs.cn/list=%s";
    private static final String SINA_REFERER = "http://finance.sina.com.cn/";
    private static final String EASTMONEY_L2_URL = "http://push2.eastmoney.com/api/qt/stock/get";
    private static final String EASTMONEY_EST_URL_TEMPLATE = "http://fundgz.1234567.com.cn/js/%s.js?rt=%d";
    
    // 正则表达式
    private static final Pattern QUOTE_PATTERN = Pattern.compile("=\"(.*?)\"");
    private static final Pattern JSONP_PATTERN = Pattern.compile("jsonpgz\\((.*?)\\);");
    
    // L2行情支持的基金代码前缀
    private static final String L2_SUPPORTED_PREFIX_REGEX = "^(15|16|50|51|56|58).*";
    
    // 交易时间配置
    private static final int TRADING_START_HOUR = 9;
    private static final int TRADING_START_MINUTE = 0;
    private static final int TRADING_END_HOUR = 20;
    private static final int TRADING_END_MINUTE = 30;
    
    // 其他常量
    private static final String DEFAULT_FUND_NAME_PREFIX = "基金";
    private static final String EMPTY_NAME = "";
    private static final int WEEKEND_START_DAY = 6; // Saturday

    /**
     * 从新浪财经获取基金数据
     * 【数据源 A：新浪财经】
     * 特点：最适合周末/盘后。
     * 核心逻辑：强制用 (最新确权净值 - 昨日净值) 计算涨跌，无视接口可能返回的0。
     * 注意：使用HTTP协议是因为新浪财经API不支持HTTPS
     *
     * @param code 基金代码
     * @return 基金数据，包含 name, gsz(当前价), dwjz(昨收价), gszzl(涨幅), date；失败返回空Map
     */
    public JSONObject fetchFromSina(String code) {
        String url = String.format(SINA_FUND_URL_TEMPLATE, code);
        
        try (HttpResponse httpResponse = HttpRequest.get(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", SINA_REFERER)
                .timeout(DEFAULT_TIMEOUT)
                .execute()) {

            String content = new String(httpResponse.bodyBytes(), Charset.forName(CHARSET_GBK));
            Matcher matcher = QUOTE_PATTERN.matcher(content);

            if (matcher.find()) {
                String[] data = matcher.group(1).split(",");
                if (data.length > 4) {
                    return buildSinaFundData(data);
                }
            }
        } catch (Exception e) {
            log.warn("获取新浪财经数据失败 - 基金代码: {}, 错误: {}", code, e.getMessage());
        }
        return createEmptyResult();
    }

    /**
     * 构建新浪基金数据对象
     */
    private JSONObject buildSinaFundData(String[] data) {
        String name = data[0];
        double currentPrice = Double.parseDouble(data[1]);  // 最新确权净值
        double prevPrice = Double.parseDouble(data[3]);     // 上次确权净值
        String date = data[4];

        // 强制计算涨跌幅
        double calcGszzl = calculateChangePercent(currentPrice, prevPrice);

        JSONObject result = new JSONObject();
        result.put("source", SOURCE_SINA);
        result.put("name", name);
        result.put("gsz", currentPrice);
        result.put("dwjz", prevPrice);
        result.put("gszzl", calcGszzl);
        result.put("date", date);
        result.put("status", STATUS_CLOSED);
        return result;
    }

    /**
     * 从L2实时行情获取基金数据
     * 【数据源 B：L2 实时行情】
     * 特点：最适合盘中。
     * 核心逻辑：强制用 (f43现价 - f60昨收) 计算。
     * <p>
     * 注意：使用HTTP协议是因为东方财富API不支持HTTPS
     *
     * @param code 基金代码
     * @return 基金数据，包含 name, gsz(现价), dwjz(昨收), gszzl(涨幅)；失败返回空Map
     */
    public JSONObject fetchL2Market(String code) {
        // 只支持特定代码开头的基金
        if (!code.matches(L2_SUPPORTED_PREFIX_REGEX)) {
            return createEmptyResult();
        }

        try (HttpResponse httpResponse = HttpRequest.get(EASTMONEY_L2_URL)
                .form("secid", buildSecId(code))
                .form("fields", "f43,f60,f170")  // f43现价, f60昨收, f170官方涨幅
                .form("invt", "2")
                .form("_", System.currentTimeMillis())
                .timeout(SHORT_TIMEOUT)
                .execute()) {

            JSONObject data = JSON.parseObject(httpResponse.body()).getJSONObject("data");
            if (data != null && !"-".equals(data.getString("f43"))) {
                return buildL2MarketData(data);
            }
        } catch (Exception e) {
            log.warn("获取L2实时行情数据失败 - 基金代码: {}, 错误: {}", code, e.getMessage());
        }
        return createEmptyResult();
    }

    /**
     * 构建证券ID（secid）
     * 5开头用1.前缀，其他用0.前缀
     */
    private String buildSecId(String code) {
        String prefix = code.startsWith("5") ? "1." : "0.";
        return prefix + code;
    }

    /**
     * 构建L2行情数据对象
     */
    private JSONObject buildL2MarketData(JSONObject data) {
        double currentPrice = data.getDoubleValue("f43");
        double prevPrice = data.getDoubleValue("f60");
        double apiRate = data.getDoubleValue("f170");

        // 优先用官方涨幅，如果官方是0但价格不一致，手动算
        if (apiRate == 0 && currentPrice != prevPrice) {
            apiRate = calculateChangePercent(currentPrice, prevPrice);
        }

        JSONObject result = new JSONObject();
        result.put("source", SOURCE_L2);
        result.put("name", EMPTY_NAME);  // L2行情不提供名称
        result.put("gsz", currentPrice);
        result.put("dwjz", prevPrice);
        result.put("gszzl", apiRate);
        result.put("status", STATUS_TRADING);
        return result;
    }

    /**
     * 从天天基金获取实时估值
     * 【数据源 C：天天基金估算】
     * 特点：盘中估值参考。
     * <p>
     * 注意：使用HTTP协议是因为天天基金API不支持HTTPS
     *
     * @param code 基金代码
     * @return 基金数据，包含 name, gsz(实时估值), dwjz(昨日净值), gszzl(估算涨幅), gztime；失败返回空Map
     */
    public JSONObject fetchEastMoneyEstimate(String code) {
        String url = String.format(EASTMONEY_EST_URL_TEMPLATE, code, System.currentTimeMillis());
        
        try (HttpResponse httpResponse = HttpRequest.get(url)
                .timeout(DEFAULT_TIMEOUT)
                .execute()) {

            Matcher matcher = JSONP_PATTERN.matcher(httpResponse.body());
            if (matcher.find()) {
                JSONObject data = JSON.parseObject(matcher.group(1));
                return buildEastMoneyEstData(data);
            }
        } catch (Exception e) {
            log.warn("获取天天基金估算数据失败 - 基金代码: {}, 错误: {}", code, e.getMessage());
        }
        return createEmptyResult();
    }

    /**
     * 构建天天基金估算数据对象
     */
    private JSONObject buildEastMoneyEstData(JSONObject data) {
        JSONObject result = new JSONObject();
        result.put("source", SOURCE_EASTMONEY);
        result.put("name", data.getString("name"));
        result.put("gsz", data.getDoubleValue("gsz"));
        result.put("dwjz", data.getDoubleValue("dwjz"));
        result.put("gszzl", data.getDoubleValue("gszzl"));
        result.put("gztime", data.getString("gztime"));
        
        String gztime = data.getString("gztime");
        result.put("date", gztime != null && gztime.length() >= 10 ? gztime.substring(0, 10) : EMPTY_NAME);
        result.put("status", STATUS_TRADING);
        return result;
    }

    /**
     * 从新浪财经获取指数数据
     *
     * @param code 指数代码（如：sh000001）
     * @return 指数数据，包含 name(指数名称), current(当前点位), change(涨跌点数), changePct(涨跌幅)；失败返回空Map
     */
    public JSONObject fetchIndexData(String code) {
        String url = String.format(SINA_INDEX_URL_TEMPLATE, code);
        
        try (HttpResponse httpResponse = HttpRequest.get(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", SINA_REFERER)
                .timeout(LONG_TIMEOUT)
                .execute()) {

            String content = new String(httpResponse.bodyBytes(), Charset.forName(CHARSET_GBK));
            Matcher matcher = QUOTE_PATTERN.matcher(content);

            if (matcher.find()) {
                String[] data = matcher.group(1).split(",");
                if (data.length > 3) {
                    return buildIndexData(data);
                }
            }
        } catch (Exception e) {
            log.warn("获取指数数据失败 - 指数代码: {}, 错误: {}", code, e.getMessage());
        }
        return createEmptyResult();
    }

    /**
     * 构建指数数据对象
     */
    private JSONObject buildIndexData(String[] data) {
        String name = data[0];
        double current = Double.parseDouble(data[3]);      // 当前点位
        double prevClose = Double.parseDouble(data[2]);    // 昨收
        double change = current - prevClose;               // 涨跌点数
        double changePct = calculateChangePercent(current, prevClose);

        JSONObject result = new JSONObject();
        result.put("name", name);
        result.put("current", current);
        result.put("change", change);
        result.put("changePct", changePct);
        return result;
    }

    /**
     * 判断当前是否为基金交易时间
     * 基金交易时间：周一至周五 09:00-20:30
     *
     * @return true=交易时间, false=非交易时间
     */
    private boolean isFundTradingTime() {
        LocalTime now = LocalTime.now();
        int dayOfWeek = java.time.LocalDate.now().getDayOfWeek().getValue(); // 1=Monday, 7=Sunday

        // 周末不交易
        if (dayOfWeek >= WEEKEND_START_DAY) {
            return false;
        }

        // 工作日交易时间段判断：09:00-20:30
        LocalTime tradingStart = LocalTime.of(TRADING_START_HOUR, TRADING_START_MINUTE);
        LocalTime tradingEnd = LocalTime.of(TRADING_END_HOUR, TRADING_END_MINUTE);

        return now.isAfter(tradingStart) && now.isBefore(tradingEnd);
    }

    /**
     * 获取最佳基金数据（智能选择数据源）
     * 数据源选择策略：
     * 1️ 交易时间（09:00-20:30）：
     * - 优先：L2 实时行情数据（如果支持）
     * - 辅助：天天基金实时估算数据
     * 2️ 交易结束后（20:30之后）：
     * - 统一：新浪财经数据源（官方净值）
     *
     * @param code 基金代码
     * @return 基金数据；所有数据源均失败时返回空Map
     */
    public JSONObject getBestFundData(String code) {
        boolean isTradingTime = isFundTradingTime();
        
        if (isTradingTime) {
            return fetchTradingTimeData(code);
        } else {
            return fetchNonTradingTimeData(code);
        }
    }

    /**
     * 获取交易时间的基金数据
     */
    private JSONObject fetchTradingTimeData(String code) {
        // 1. 优先尝试 L2 实时行情（如果支持）
        JSONObject l2Data = fetchL2Market(code);
        if (!l2Data.isEmpty()) {
            // 补全基金名称
            l2Data.put("name", getFundName(code));
            log.debug("交易时间，使用L2实时行情数据 - 基金代码: {}", code);
            return l2Data;
        }

        // 2. 辅助使用天天基金实时估算
        JSONObject eastData = fetchEastMoneyEstimate(code);
        if (!eastData.isEmpty()) {
            log.debug("交易时间，使用天天基金实时估算 - 基金代码: {}", code);
            return eastData;
        }

        // 3. 降级：如果以上都失败，使用新浪财经
        JSONObject sinaData = fetchFromSina(code);
        if (!sinaData.isEmpty()) {
            log.debug("交易时间，L2和天天基金均失败，降级使用新浪财经 - 基金代码: {}", code);
            return sinaData;
        }

        log.warn("所有数据源均失败 - 基金代码: {}", code);
        return createEmptyResult();
    }

    /**
     * 获取非交易时间的基金数据
     */
    private JSONObject fetchNonTradingTimeData(String code) {
        // 统一使用新浪财经数据源（官方净值）
        JSONObject sinaData = fetchFromSina(code);
        if (!sinaData.isEmpty()) {
            log.debug("非交易时间，使用新浪财经官方净值 - 基金代码: {}", code);
            return sinaData;
        }

        // 降级：如果新浪失败，尝试天天基金
        JSONObject eastData = fetchEastMoneyEstimate(code);
        if (!eastData.isEmpty()) {
            log.debug("非交易时间，新浪财经失败，降级使用天天基金 - 基金代码: {}", code);
            return eastData;
        }

        log.warn("所有数据源均失败 - 基金代码: {}", code);
        return createEmptyResult();
    }

    /**
     * 获取基金名称（从多个数据源尝试）
     */
    private String getFundName(String code) {
        JSONObject sinaData = fetchFromSina(code);
        if (!sinaData.isEmpty() && sinaData.getString("name") != null) {
            return sinaData.getString("name");
        }

        JSONObject eastData = fetchEastMoneyEstimate(code);
        if (!eastData.isEmpty() && eastData.getString("name") != null) {
            return eastData.getString("name");
        }

        return DEFAULT_FUND_NAME_PREFIX + code;
    }

    // ==================== 工具方法 ====================

    /**
     * 计算涨跌幅百分比
     * 
     * @param current 当前价格
     * @param prev 前一价格
     * @return 涨跌幅（百分比）
     */
    private double calculateChangePercent(double current, double prev) {
        if (prev <= 0) {
            return 0;
        }
        return (current - prev) / prev * 100;
    }

    /**
     * 创建空结果对象
     * 
     * @return 空的JSONObject
     */
    private JSONObject createEmptyResult() {
        return new JSONObject(Collections.emptyMap());
    }
}
