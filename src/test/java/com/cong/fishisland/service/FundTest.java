package com.cong.fishisland.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cong.fishisland.common.TestBase;
import com.cong.fishisland.model.dto.fund.AddFundRequest;
import com.cong.fishisland.model.dto.fund.DeleteFundRequest;
import com.cong.fishisland.model.vo.fund.FundItemVO;
import com.cong.fishisland.model.vo.fund.FundListVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基金数据源测试
 * 测试三个数据源是否能成功获取基金数据：
 * 1. 新浪财经（SINA_OFFICIAL）
 * 2. L2实时行情（LEVEL2_MARKET）
 * 3. 天天基金估算（EASTMONEY_EST）
 */
@Slf4j
public class FundTest extends TestBase {

    // 测试用的基金代码
    private static final String[] TEST_FUND_CODES = {"025942"};
//    private static final String[] TEST_FUND_CODES = {"015968", "025491", "025942", "015795"};

    // 测试用的基金代码
    private static final String[] TEST_FUND_NAMES = {"平安中证卫星产业指数C", "广发新动力混合C"};


    /**
     * 测试新浪财经数据源
     */
    @Test
    void testSinaDataSource() {
        log.info("========== 开始测试新浪财经数据源 ==========");

        for (String code : TEST_FUND_CODES) {
            try {
                log.info("测试基金代码: {}", code);
                String url = "http://hq.sinajs.cn/list=f_" + code;

                // 获取 HttpResponse 对象，然后获取原始字节数组
                HttpResponse httpResponse = HttpRequest.get(url)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .header("Referer", "http://finance.sina.com.cn/")
                        .timeout(2000)
                        .execute();

                // 获取原始字节数组
                byte[] responseBytes = httpResponse.bodyBytes();

                // 使用 GBK 解码
                String content;
                try {
                    content = new String(responseBytes, Charset.forName("GBK"));
                } catch (Exception e) {
                    // 如果 GBK 解码失败，尝试 UTF-8
                    content = new String(responseBytes, StandardCharsets.UTF_8);
                    log.warn("GBK 解码失败，使用 UTF-8: {}", e.getMessage());
                }

                log.info("响应内容: {}", content);

                // 解析数据：var hq_str_f_015968="基金名称,最新净值,涨跌,昨日净值,日期,..."
                Pattern pattern = Pattern.compile("=\"(.*?)\"");
                Matcher matcher = pattern.matcher(content);

                if (matcher.find()) {
                    String dataStr = matcher.group(1);
                    String[] data = dataStr.split(",");

                    if (data.length > 4) {
                        String name = data[0];
                        double currentPrice = Double.parseDouble(data[1]);
                        double prevPrice = Double.parseDouble(data[3]);
                        String date = data[4];

                        double calcGszzl = 0;
                        if (prevPrice > 0) {
                            calcGszzl = (currentPrice - prevPrice) / prevPrice * 100;
                        }

                        log.info("✓ 新浪财经数据获取成功:");
                        log.info("  基金名称: {}", name);
                        log.info("  最新净值: {}", currentPrice);
                        log.info("  昨日净值: {}", prevPrice);
                        log.info("  涨跌幅: {}%", String.format("%.2f", calcGszzl));
                        log.info("  日期: {}", date);
                    } else {
                        log.warn("✗ 数据格式不正确，字段数量不足");
                    }
                } else {
                    log.warn("✗ 未找到有效数据");
                }

            } catch (Exception e) {
                log.error("✗ 获取新浪财经数据失败 - 基金代码: {}, 错误: {}", code, e.getMessage());
            }
            log.info("----------------------------------------");
        }

        log.info("========== 新浪财经数据源测试完成 ==========\n");
    }

    /**
     * 测试L2实时行情数据源
     */
    @Test
    void testL2MarketDataSource() {
        log.info("========== 开始测试L2实时行情数据源 ==========");
        log.info("说明：L2行情只支持特定代码开头的基金：15、16、50、51、56、58");
        log.info("API 地址：http://push2.eastmoney.com/api/qt/stock/get");
        log.info("字段说明：f43=现价, f60=昨收, f170=官方涨幅\n");

        // L2行情支持的基金代码（15、16、50、51、56、58开头）
        String[] l2SupportedCodes = {"159680", "159681", "159682", "515050", "515880", "588000"};

        for (String code : l2SupportedCodes) {
            try {
                log.info("测试基金代码: {} (符合L2行情规则)", code);

                // 确定前缀：5开头用1.，其他用0.
                String prefix = code.startsWith("5") ? "1." : "0.";
                String secid = prefix + code;
                log.info("  secid: {}", secid);

                String url = "http://push2.eastmoney.com/api/qt/stock/get";
                long timestamp = System.currentTimeMillis();

                String response = HttpRequest.get(url)
                        .form("secid", secid)
                        .form("fields", "f43,f60,f170")  // f43现价, f60昨收, f170官方涨幅
                        .form("invt", "2")
                        .form("_", timestamp)
                        .timeout(2000)
                        .execute()
                        .body();

                log.info("  响应内容: {}", response);

                JSONObject json = JSON.parseObject(response);
                JSONObject data = json.getJSONObject("data");

                if (data != null && !"-".equals(data.getString("f43"))) {
                    double currentPrice = data.getDoubleValue("f43");
                    double prevPrice = data.getDoubleValue("f60");
                    double apiRate = data.getDoubleValue("f170");

                    // 如果官方涨幅为0但价格不一致，手动计算
                    if (apiRate == 0 && currentPrice != prevPrice && prevPrice > 0) {
                        double calcRate = (currentPrice - prevPrice) / prevPrice * 100;
                        log.info("  官方涨幅为0，手动计算: {}%", String.format("%.2f", calcRate));
                        apiRate = calcRate;
                    }

                    log.info("✓ L2实时行情数据获取成功:");
                    log.info("  现价 (f43): {}", currentPrice);
                    log.info("  昨收 (f60): {}", prevPrice);
                    log.info("  涨幅 (f170): {}%", String.format("%.2f", apiRate));
                    log.info("  数据来源: LEVEL2_MARKET");
                    log.info("  状态: trading");
                } else {
                    log.warn("✗ 未找到有效数据或数据为空");
                }

            } catch (Exception e) {
                log.error("✗ 获取L2实时行情数据失败 - 基金代码: {}, 错误: {}", code, e.getMessage());
            }
            log.info("----------------------------------------");
        }

        // 测试不支持的代码
        log.info("测试不支持L2行情的基金代码:");
        for (String code : TEST_FUND_CODES) {
            if (!code.matches("^(15|16|50|51|56|58).*")) {
                log.info("  基金代码 {} 不符合L2行情规则（不是15/16/50/51/56/58开头），应该返回null", code);
                // 这里可以调用实际的方法验证
            }
        }

        log.info("========== L2实时行情数据源测试完成 ==========\n");
    }

    /**
     * 测试天天基金估算数据源（实时估值）
     * <p>
     * 实时估值来源说明：
     * 1. API 地址：http://fundgz.1234567.com.cn/js/{基金代码}.js?rt={时间戳}
     * 2. 返回格式：JSONP 格式，如 jsonpgz({...});
     * 3. 数据来源：天天基金网提供的基金实时估值服务
     * 4. 估值原理：根据基金持仓股票的最新价格，实时计算基金净值估算值
     * 5. 适用场景：交易时间内（9:30-15:00）提供实时估值，非交易时间显示最新净值
     * <p>
     * 返回的 JSON 字段说明：
     * - name: 基金名称
     * - gsz: 实时估值（这就是"实时估值"字段，根据持仓股票实时价格计算）
     * - dwjz: 昨日净值（上一个交易日的确认净值）
     * - gszzl: 估算涨幅（(实时估值-昨日净值)/昨日净值*100）
     * - gztime: 估算时间
     */
    @Test
    void testEastMoneyEstimateDataSource() {
        log.info("========== 开始测试天天基金估算数据源（实时估值） ==========");
        log.info("说明：实时估值是根据基金持仓股票的最新价格实时计算的估算值");
        log.info("API 地址格式：http://fundgz.1234567.com.cn/js/{基金代码}.js?rt={时间戳}");
        log.info("返回格式：jsonpgz({...}); （JSONP 格式）\n");

        for (String code : TEST_FUND_CODES) {
            try {
                log.info("测试基金代码: {}", code);
                long timestamp = System.currentTimeMillis();
                String url = "http://fundgz.1234567.com.cn/js/" + code + ".js?rt=" + timestamp;

                String response = HttpRequest.get(url)
                        .timeout(2000)
                        .execute()
                        .body();

                log.info("响应内容: {}", response);

                // 解析数据：jsonpgz({...});
                Pattern pattern = Pattern.compile("jsonpgz\\((.*?)\\);");
                Matcher matcher = pattern.matcher(response);

                if (matcher.find()) {
                    String jsonStr = matcher.group(1);
                    JSONObject data = JSON.parseObject(jsonStr);

                    String name = data.getString("name");
                    double gsz = data.getDoubleValue("gsz");        // 实时估值（根据持仓股票实时价格计算）
                    double dwjz = data.getDoubleValue("dwjz");      // 昨日净值（上一个交易日确认的净值）
                    double gszzl = data.getDoubleValue("gszzl");   // 估算涨幅
                    String gztime = data.getString("gztime");        // 估算时间

                    log.info("✓ 天天基金估算数据获取成功:");
                    log.info("  基金名称: {}", name);
                    log.info("  实时估值 (gsz): {} ← 这就是实时估值，根据持仓股票实时价格计算", gsz);
                    log.info("  昨日净值 (dwjz): {} ← 上一个交易日确认的净值", dwjz);
                    log.info("  估算涨幅 (gszzl): {}% ← (实时估值-昨日净值)/昨日净值*100", String.format("%.2f", gszzl));
                    log.info("  估算时间 (gztime): {}", gztime);

                    // 打印原始 JSON 数据，方便查看所有字段
                    log.info("  原始 JSON 数据: {}", jsonStr);
                } else {
                    log.warn("✗ 未找到有效数据");
                }

            } catch (Exception e) {
                log.error("✗ 获取天天基金估算数据失败 - 基金代码: {}, 错误: {}", code, e.getMessage());
            }
            log.info("----------------------------------------");
        }

        log.info("========== 天天基金估算数据源测试完成 ==========\n");
    }

    /**
     * 综合测试：测试所有数据源
     */
    @Test
    void testAllDataSources() {
        log.info("========== 开始综合测试所有数据源 ==========\n");

        testSinaDataSource();
        testL2MarketDataSource();
        testEastMoneyEstimateDataSource();

        log.info("========== 所有数据源测试完成 ==========");
    }

    @Resource
    private FundService fundService;

    @Resource
    private com.cong.fishisland.service.fund.FundDataService fundDataService;

    /**
     * 测试FundDataService的所有方法
     */
    @Test
    void testFundDataServiceMethods() {
        log.info("========== 开始测试FundDataService所有方法 ==========");

        String testCode = "015968";

        // 1. 测试新浪财经数据源
        log.info("1. 测试新浪财经数据源");
        JSONObject sinaData = fundDataService.fetchFromSina(testCode);
        if (sinaData != null) {
            log.info("✓ 新浪财经数据获取成功:");
            log.info("  数据源: {}", sinaData.getString("source"));
            log.info("  基金名称: {}", sinaData.getString("name"));
            log.info("  当前价格: {}", sinaData.getDoubleValue("gsz"));
            log.info("  昨日净值: {}", sinaData.getDoubleValue("dwjz"));
            log.info("  涨跌幅: {}%", String.format("%.2f", sinaData.getDoubleValue("gszzl")));
            log.info("  日期: {}", sinaData.getString("date"));
            log.info("  状态: {}", sinaData.getString("status"));
        } else {
            log.warn("✗ 新浪财经数据获取失败");
        }

        // 2. 测试天天基金估算数据源
        log.info("\n2. 测试天天基金估算数据源");
        JSONObject eastData = fundDataService.fetchEastMoneyEstimate(testCode);
        if (eastData != null) {
            log.info("✓ 天天基金估算数据获取成功:");
            log.info("  数据源: {}", eastData.getString("source"));
            log.info("  基金名称: {}", eastData.getString("name"));
            log.info("  实时估值: {}", eastData.getDoubleValue("gsz"));
            log.info("  昨日净值: {}", eastData.getDoubleValue("dwjz"));
            log.info("  估算涨幅: {}%", String.format("%.2f", eastData.getDoubleValue("gszzl")));
            log.info("  估算时间: {}", eastData.getString("gztime"));
            log.info("  状态: {}", eastData.getString("status"));
        } else {
            log.warn("✗ 天天基金估算数据获取失败");
        }

        // 3. 测试L2实时行情数据源（使用支持的代码）
        log.info("\n3. 测试L2实时行情数据源");
        String l2Code = "159680"; // 使用支持L2的代码
        JSONObject l2Data = fundDataService.fetchL2Market(l2Code);
        if (l2Data != null) {
            log.info("✓ L2实时行情数据获取成功:");
            log.info("  数据源: {}", l2Data.getString("source"));
            log.info("  基金代码: {}", l2Code);
            log.info("  现价: {}", l2Data.getDoubleValue("gsz"));
            log.info("  昨收: {}", l2Data.getDoubleValue("dwjz"));
            log.info("  涨跌幅: {}%", String.format("%.2f", l2Data.getDoubleValue("gszzl")));
            log.info("  状态: {}", l2Data.getString("status"));
        } else {
            log.warn("✗ L2实时行情数据获取失败（可能是代码不支持或市场未开放）");
        }

        // 测试不支持L2的代码
        log.info("\n测试不支持L2的基金代码: {}", testCode);
        JSONObject unsupportedL2 = fundDataService.fetchL2Market(testCode);
        if (unsupportedL2 == null) {
            log.info("✓ 正确返回null（代码不支持L2行情）");
        } else {
            log.warn("✗ 应该返回null但返回了数据");
        }

        // 4. 测试智能选择数据源
        log.info("\n4. 测试智能选择数据源 (getBestFundData)");
        JSONObject bestData = fundDataService.getBestFundData(testCode);
        if (bestData != null) {
            log.info("✓ 智能选择数据源成功:");
            log.info("  选择的数据源: {}", bestData.getString("source"));
            log.info("  基金名称: {}", bestData.getString("name"));
            log.info("  当前价格: {}", bestData.getDoubleValue("gsz"));
            log.info("  昨日净值: {}", bestData.getDoubleValue("dwjz"));
            log.info("  涨跌幅: {}%", String.format("%.2f", bestData.getDoubleValue("gszzl")));

            // 根据数据源显示特定字段
            String source = bestData.getString("source");
            if ("SINA_OFFICIAL".equals(source)) {
                log.info("  日期: {}", bestData.getString("date"));
            } else if ("EASTMONEY_EST".equals(source)) {
                log.info("  估算时间: {}", bestData.getString("gztime"));
            }
            log.info("  状态: {}", bestData.getString("status"));
        } else {
            log.warn("✗ 智能选择数据源失败");
        }

        log.info("========== FundDataService所有方法测试完成 ==========\n");
    }

    /**
     * 测试添加基金功能
     * 模拟用户输入基金代码和基金总金额，验证是否能正确计算实时涨跌幅度和涨跌金额
     * <p>
     * 测试场景：
     * 1. 添加基金（输入基金代码、持有金额、盈亏金额）
     * 2. 获取基金列表，验证实时数据计算
     * 3. 验证持仓总市值、今日盈亏、累计盈亏等计算结果
     */
    @Test
    void testAddFundAndCalculateProfit() {
        log.info("========== 开始测试添加基金功能 ==========");

        // 模拟用户ID（测试环境）
        Long testUserId = 999999L;

        // 测试基金代码（使用实际存在的基金代码）
        String fundCode = "015968";
        BigDecimal holdingAmount = new BigDecimal("174.59");  // 持有金额
        BigDecimal profit = new BigDecimal("3.88");  // 盈亏金额（正数为盈利）

        try {
            // 1. 添加基金
            log.info("步骤1: 添加基金");
            log.info("  基金代码: {}", fundCode);
            log.info("  持有金额: {}", holdingAmount);
            log.info("  盈亏金额: {}", profit);

            AddFundRequest addFundRequest = new AddFundRequest();
            addFundRequest.setCode(fundCode);
            addFundRequest.setAmount(holdingAmount);
            addFundRequest.setProfit(profit);

            Boolean addResult = fundService.addFund(addFundRequest, testUserId);
            Assertions.assertTrue(addResult, "添加基金应该成功");
            log.info("✓ 基金添加成功");

            // 2. 获取基金列表，验证计算结果
            log.info("\n步骤2: 获取基金列表并验证计算结果");
            FundListVO fundListVO = fundService.getFundList(testUserId);

            Assertions.assertNotNull(fundListVO, "基金列表不应为空");
            Assertions.assertNotNull(fundListVO.getFundList(), "基金列表数据不应为空");
            Assertions.assertFalse(fundListVO.getFundList().isEmpty(), "基金列表不应为空");

            // 3. 验证汇总数据
            log.info("\n步骤3: 验证汇总数据");
            log.info("  持仓总市值: ¥ {}", fundListVO.getTotalMarketValue());
            log.info("  今日预估收益: ¥ {}", fundListVO.getTotalDayProfit());
            log.info("  历史累计收益: ¥ {}", fundListVO.getTotalProfit());

            Assertions.assertNotNull(fundListVO.getTotalMarketValue(), "总市值不应为空");
            Assertions.assertNotNull(fundListVO.getTotalDayProfit(), "今日盈亏不应为空");
            Assertions.assertNotNull(fundListVO.getTotalProfit(), "累计盈亏不应为空");

            // 4. 验证单个基金数据
            log.info("\n步骤4: 验证单个基金数据");
            FundItemVO fundItem = fundListVO.getFundList().get(0);

            log.info("  基金代码: {}", fundItem.getCode());
            log.info("  基金名称: {}", fundItem.getName());
            log.info("  持有份额: {}", fundItem.getShares());
            log.info("  成本价: {}", fundItem.getCost());
            log.info("  当前价格（实时估值）: {}", fundItem.getCurrentPrice());
            log.info("  昨日净值: {}", fundItem.getPrevPrice());
            log.info("  涨跌幅: {}%", fundItem.getChangePercent());
            log.info("  持有市值: ¥ {}", fundItem.getMarketValue());
            log.info("  今日盈亏: ¥ {}", fundItem.getDayProfit());
            log.info("  持有盈亏: ¥ {}", fundItem.getTotalProfit());
            log.info("  更新时间: {}", fundItem.getUpdateTime());

            // 验证基金代码匹配
            Assertions.assertEquals(fundCode, fundItem.getCode(), "基金代码应该匹配");

            // 验证计算逻辑
            // 持有市值 = 份额 × 当前价格
            BigDecimal expectedMarketValue = fundItem.getShares().multiply(fundItem.getCurrentPrice());
            Assertions.assertTrue(
                    fundItem.getMarketValue().subtract(expectedMarketValue).abs().compareTo(new BigDecimal("0.01")) < 0,
                    "持有市值计算应该正确"
            );

            // 今日盈亏 = (当前价格 - 昨日净值) × 份额
            BigDecimal expectedDayProfit = fundItem.getCurrentPrice()
                    .subtract(fundItem.getPrevPrice())
                    .multiply(fundItem.getShares());
            Assertions.assertTrue(
                    fundItem.getDayProfit().subtract(expectedDayProfit).abs().compareTo(new BigDecimal("0.01")) < 0,
                    "今日盈亏计算应该正确"
            );

            // 累计盈亏 = (当前价格 - 成本价) × 份额
            BigDecimal expectedTotalProfit = fundItem.getCurrentPrice()
                    .subtract(fundItem.getCost())
                    .multiply(fundItem.getShares());
            Assertions.assertTrue(
                    fundItem.getTotalProfit().subtract(expectedTotalProfit).abs().compareTo(new BigDecimal("0.01")) < 0,
                    "累计盈亏计算应该正确"
            );

            // 验证汇总数据与单个基金数据一致
            Assertions.assertTrue(
                    fundListVO.getTotalMarketValue().subtract(fundItem.getMarketValue()).abs().compareTo(new BigDecimal("0.01")) < 0,
                    "总市值应该等于单个基金的市值"
            );
            Assertions.assertTrue(
                    fundListVO.getTotalDayProfit().subtract(fundItem.getDayProfit()).abs().compareTo(new BigDecimal("0.01")) < 0,
                    "总今日盈亏应该等于单个基金的今日盈亏"
            );
            Assertions.assertTrue(
                    fundListVO.getTotalProfit().subtract(fundItem.getTotalProfit()).abs().compareTo(new BigDecimal("0.01")) < 0,
                    "总累计盈亏应该等于单个基金的累计盈亏"
            );

            log.info("\n✓ 所有验证通过！");
            log.info("\n========== 测试结果汇总 ==========");
            log.info("持仓总市值: ¥ {}", fundListVO.getTotalMarketValue());
            log.info("今日预估收益: ¥ {} ({})",
                    fundListVO.getTotalDayProfit(),
                    fundListVO.getTotalDayProfit().compareTo(BigDecimal.ZERO) >= 0 ? "盈利" : "亏损");
            log.info("历史累计收益: ¥ {} ({})",
                    fundListVO.getTotalProfit(),
                    fundListVO.getTotalProfit().compareTo(BigDecimal.ZERO) >= 0 ? "盈利" : "亏损");
            log.info("实时涨跌幅: {}%", fundItem.getChangePercent());
            log.info("=====================================");

        } catch (Exception e) {
            log.error("测试失败: {}", e.getMessage(), e);
            Assertions.fail("测试应该成功，但出现异常: " + e.getMessage());
        } finally {
            // 清理测试数据（可选）
            try {
                DeleteFundRequest deleteRequest = new DeleteFundRequest();
                deleteRequest.setCode(fundCode);
                fundService.deleteFund(deleteRequest, testUserId);
                log.info("测试数据已清理");
            } catch (Exception e) {
                log.warn("清理测试数据失败: {}", e.getMessage());
            }
        }

        log.info("========== 添加基金功能测试完成 ==========\n");
    }
}

