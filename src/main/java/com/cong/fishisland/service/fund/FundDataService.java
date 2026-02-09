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
        HttpResponse httpResponse = null;
        try {
            // 注意：新浪财经API仅支持HTTP协议
            String url = "http://hq.sinajs.cn/list=f_" + code;

            httpResponse = HttpRequest.get(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Referer", "http://finance.sina.com.cn/")
                    .timeout(2000)
                    .execute();

            byte[] responseBytes = httpResponse.bodyBytes();
            String content = new String(responseBytes, Charset.forName("GBK"));

            Pattern pattern = Pattern.compile("=\"(.*?)\"");
            Matcher matcher = pattern.matcher(content);

            if (matcher.find()) {
                String dataStr = matcher.group(1);
                String[] data = dataStr.split(",");

                if (data.length > 4) {
                    String name = data[0];
                    // data[1]: 最新确权净值 (例如周五的净值)
                    // data[3]: 上次确权净值 (例如周四的净值)
                    double currentPrice = Double.parseDouble(data[1]);
                    double prevPrice = Double.parseDouble(data[3]);
                    String date = data[4];

                    // 强制计算涨跌幅
                    double calcGszzl = 0;
                    if (prevPrice > 0) {
                        calcGszzl = (currentPrice - prevPrice) / prevPrice * 100;
                    }

                    JSONObject result = new JSONObject();
                    result.put("source", "SINA_OFFICIAL");
                    result.put("name", name);
                    result.put("gsz", currentPrice);
                    result.put("dwjz", prevPrice);
                    result.put("gszzl", calcGszzl);
                    result.put("date", date);
                    result.put("status", "closed");
                    return result;
                }
            }
        } catch (Exception e) {
            log.warn("获取新浪财经数据失败 - 基金代码: {}, 错误: {}", code, e.getMessage());
        } finally {
            // 关闭HttpResponse资源
            if (httpResponse != null) {
                httpResponse.close();
            }
        }
        return new JSONObject(Collections.emptyMap());
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
        // 只支持特定代码开头的基金：15、16、50、51、56、58
        if (!code.matches("^(15|16|50|51|56|58).*")) {
            return new JSONObject(Collections.emptyMap());
        }

        HttpResponse httpResponse = null;
        try {
            // 确定前缀：5开头用1.，其他用0.
            String prefix = code.startsWith("5") ? "1." : "0.";
            String secid = prefix + code;

            // 注意：东方财富API仅支持HTTP协议
            String url = "http://push2.eastmoney.com/api/qt/stock/get";
            long timestamp = System.currentTimeMillis();

            httpResponse = HttpRequest.get(url)
                    .form("secid", secid)
                    .form("fields", "f43,f60,f170")  // f43现价, f60昨收, f170官方涨幅
                    .form("invt", "2")
                    .form("_", timestamp)
                    .timeout(1000)
                    .execute();

            String response = httpResponse.body();
            JSONObject json = JSON.parseObject(response);
            JSONObject data = json.getJSONObject("data");

            if (data != null && !"-".equals(data.getString("f43"))) {
                double currentPrice = data.getDoubleValue("f43");
                double prevPrice = data.getDoubleValue("f60");

                // 优先用官方涨幅，如果官方是0但价格不一致，手动算
                double apiRate = data.getDoubleValue("f170");
                if (apiRate == 0 && currentPrice != prevPrice && prevPrice > 0) {
                    apiRate = (currentPrice - prevPrice) / prevPrice * 100;
                }

                JSONObject result = new JSONObject();
                result.put("source", "LEVEL2_MARKET");
                result.put("name", "");  // L2行情不提供名称
                result.put("gsz", currentPrice);
                result.put("dwjz", prevPrice);
                result.put("gszzl", apiRate);
                result.put("status", "trading");
                return result;
            }
        } catch (Exception e) {
            log.warn("获取L2实时行情数据失败 - 基金代码: {}, 错误: {}", code, e.getMessage());
        } finally {
            // 关闭HttpResponse资源
            if (httpResponse != null) {
                httpResponse.close();
            }
        }
        return new JSONObject(Collections.emptyMap());
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
        HttpResponse httpResponse = null;
        try {
            long timestamp = System.currentTimeMillis();
            // 注意：天天基金API仅支持HTTP协议
            String url = "http://fundgz.1234567.com.cn/js/" + code + ".js?rt=" + timestamp;

            httpResponse = HttpRequest.get(url)
                    .timeout(2000)
                    .execute();

            String response = httpResponse.body();
            Pattern pattern = Pattern.compile("jsonpgz\\((.*?)\\);");
            Matcher matcher = pattern.matcher(response);

            if (matcher.find()) {
                String jsonStr = matcher.group(1);
                JSONObject data = JSON.parseObject(jsonStr);

                JSONObject result = new JSONObject();
                result.put("source", "EASTMONEY_EST");
                result.put("name", data.getString("name"));
                result.put("gsz", data.getDoubleValue("gsz"));
                result.put("dwjz", data.getDoubleValue("dwjz"));
                result.put("gszzl", data.getDoubleValue("gszzl"));
                result.put("gztime", data.getString("gztime"));
                result.put("date", data.getString("gztime") != null ? data.getString("gztime").substring(0, 10) : "");
                result.put("status", "trading");
                return result;
            }
        } catch (Exception e) {
            log.warn("获取天天基金估算数据失败 - 基金代码: {}, 错误: {}", code, e.getMessage());
        } finally {
            // 关闭HttpResponse资源
            if (httpResponse != null) {
                httpResponse.close();
            }
        }
        return new JSONObject(Collections.emptyMap());
    }

    /**
     * 从新浪财经获取指数数据
     *
     * @param code 指数代码（如：sh000001）
     * @return 指数数据，包含 name(指数名称), current(当前点位), change(涨跌点数), changePct(涨跌幅)；失败返回空Map
     */
    public JSONObject fetchIndexData(String code) {
        HttpResponse httpResponse = null;
        try {
            String url = "http://hq.sinajs.cn/list=" + code;

            httpResponse = HttpRequest.get(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Referer", "http://finance.sina.com.cn/")
                    .timeout(3000)
                    .execute();

            byte[] responseBytes = httpResponse.bodyBytes();
            String content = new String(responseBytes, Charset.forName("GBK"));

            // 解析数据：var hq_str_sh000001="上证指数,3000.00,10.00,2990.00,..."
            Pattern pattern = Pattern.compile("=\"(.*?)\"");
            Matcher matcher = pattern.matcher(content);

            if (matcher.find()) {
                String dataStr = matcher.group(1);
                String[] data = dataStr.split(",");

                if (data.length > 3) {
                    String name = data[0];
                    double current = Double.parseDouble(data[3]);      // 当前点位
                    double prevClose = Double.parseDouble(data[2]);    // 昨收
                    double change = current - prevClose;                // 涨跌点数
                    double changePct = prevClose > 0 ? (change / prevClose * 100) : 0;  // 涨跌幅

                    JSONObject result = new JSONObject();
                    result.put("name", name);
                    result.put("current", current);
                    result.put("change", change);
                    result.put("changePct", changePct);
                    return result;
                }
            }
        } catch (Exception e) {
            log.warn("获取指数数据失败 - 指数代码: {}, 错误: {}", code, e.getMessage());
        } finally {
            if (httpResponse != null) {
                httpResponse.close();
            }
        }
        return new JSONObject(Collections.emptyMap());
    }

    /**
     * 判断当前是否为基金交易时间
     * 基金交易时间：周一至周五 09:00-15:00
     *
     * @return true=交易时间, false=非交易时间
     */
    private boolean isFundTradingTime() {
        LocalTime now = LocalTime.now();
        int dayOfWeek = java.time.LocalDate.now().getDayOfWeek().getValue(); // 1=Monday, 7=Sunday

        // 周末不交易
        if (dayOfWeek >= 6) { // 6=Saturday, 7=Sunday
            return false;
        }

        // 工作日交易时间段判断：09:00-15:00
        LocalTime tradingStart = LocalTime.of(9, 0);   // 09:00
        LocalTime tradingEnd = LocalTime.of(15, 0);    // 15:00

        return now.isAfter(tradingStart) && now.isBefore(tradingEnd);
    }

    /**
     * 获取最佳基金数据（智能选择数据源）
     * 数据源选择策略：
     * 1️ 交易时间（09:00-15:00）：
     * - 优先：L2 实时行情数据（如果支持）
     * - 辅助：天天基金实时估算数据
     * 2️ 交易结束后（15:00之后）：
     * - 统一：新浪财经数据源（官方净值）
     *
     * @param code 基金代码
     * @return 基金数据；所有数据源均失败时返回空Map
     */
    public JSONObject getBestFundData(String code) {
        boolean isTradingTime = isFundTradingTime();
        if (isTradingTime) {
            // ========== 交易时间（09:00-15:00）==========

            // 1. 优先尝试 L2 实时行情（如果支持）
            JSONObject l2Data = fetchL2Market(code);
            if (l2Data != null && !l2Data.isEmpty()) {
                // 补全基金名称
                JSONObject sinaData = fetchFromSina(code);
                JSONObject eastData = fetchEastMoneyEstimate(code);
                String name = "基金" + code;
                if (sinaData != null && !sinaData.isEmpty() && sinaData.getString("name") != null) {
                    name = sinaData.getString("name");
                } else if (eastData != null && !eastData.isEmpty() && eastData.getString("name") != null) {
                    name = eastData.getString("name");
                }
                l2Data.put("name", name);
                log.debug("交易时间，使用L2实时行情数据 - 基金代码: {}", code);
                return l2Data;
            }

            // 2. 辅助使用天天基金实时估算
            JSONObject eastData = fetchEastMoneyEstimate(code);
            if (eastData != null && !eastData.isEmpty()) {
                log.debug("交易时间，使用天天基金实时估算 - 基金代码: {}", code);
                return eastData;
            }

            // 3. 降级：如果以上都失败，使用新浪财经
            JSONObject sinaData = fetchFromSina(code);
            if (sinaData != null && !sinaData.isEmpty()) {
                log.debug("交易时间，L2和天天基金均失败，降级使用新浪财经 - 基金代码: {}", code);
                return sinaData;
            }
        } else {
            // ========== 交易结束后（15:00之后）==========

            // 统一使用新浪财经数据源（官方净值）
            JSONObject sinaData = fetchFromSina(code);
            if (sinaData != null && !sinaData.isEmpty()) {
                log.debug("非交易时间，使用新浪财经官方净值 - 基金代码: {}", code);
                return sinaData;
            }

            // 降级：如果新浪失败，尝试天天基金
            JSONObject eastData = fetchEastMoneyEstimate(code);
            if (eastData != null && !eastData.isEmpty()) {
                log.debug("非交易时间，新浪财经失败，降级使用天天基金 - 基金代码: {}", code);
                return eastData;
            }
        }
        log.warn("所有数据源均失败 - 基金代码: {}", code);
        return new JSONObject(Collections.emptyMap());
    }
}
