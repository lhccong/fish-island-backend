package com.cong.fishisland.service.fund;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.fund.IndexTradeMapper;
import com.cong.fishisland.model.entity.fund.IndexTradeRecord;
import com.cong.fishisland.model.enums.fund.FundConstants;
import com.cong.fishisland.model.vo.fund.IndexChartVO;
import com.cong.fishisland.model.vo.fund.IndexTransactionVO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** Public candles are cached independently from private account trade markers. */
@Service
public class IndexChartService {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private final Cache<String, List<IndexChartVO.Candle>> cache = Caffeine.newBuilder()
            .maximumSize(15).expireAfterWrite(60, TimeUnit.SECONDS).build();
    @Resource
    private IndexTradeMapper tradeMapper;

    public IndexChartVO chart(Long userId, String rawCode, int days) {
        String code = FundConstants.normalizeIndexCode(rawCode);
        if (!FundConstants.isSupportedIndex(code) || (days != 60 && days != 120 && days != 250)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的指数或行情范围");
        }
        List<IndexChartVO.Candle> candles = cache.get(code + ":" + days, key -> fetch(code, days));
        IndexChartVO result = new IndexChartVO();
        result.setCandles(candles);
        result.setTrades(Collections.emptyList());
        if (candles.isEmpty()) return result;
        LocalDate first = LocalDate.parse(candles.get(0).getDate());
        LocalDate last = LocalDate.parse(candles.get(candles.size() - 1).getDate());
        List<IndexTradeRecord> records = tradeMapper.selectList(new LambdaQueryWrapper<IndexTradeRecord>()
                .eq(IndexTradeRecord::getUserId, userId)
                .eq(IndexTradeRecord::getIndexCode, code)
                .ge(IndexTradeRecord::getCreateTime, Date.from(first.atStartOfDay(SHANGHAI).toInstant()))
                .lt(IndexTradeRecord::getCreateTime, Date.from(last.plusDays(1).atStartOfDay(SHANGHAI).toInstant()))
                .gt(IndexTradeRecord::getNav, 0).gt(IndexTradeRecord::getShares, 0)
                .and(q -> q.eq(IndexTradeRecord::getTradeType, 1).eq(IndexTradeRecord::getStatus, 1)
                        .or(s -> s.eq(IndexTradeRecord::getTradeType, 2).in(IndexTradeRecord::getStatus, 0, 1)))
                .orderByDesc(IndexTradeRecord::getCreateTime).orderByDesc(IndexTradeRecord::getId)
                .last("LIMIT 5001"));
        result.setTruncated(records.size() > 5000);
        result.setTrades(records.stream().limit(5000).map(record -> marker(record, code)).collect(Collectors.toList()));
        return result;
    }

    protected List<IndexChartVO.Candle> fetch(String code, int days) {
        // Whitelisted index + bounded period only; never accept a caller-provided URL.
        String url = "https://web.ifzq.gtimg.cn/appstock/app/fqkline/get?param=" + code + ",day,,," + days + ",qfq";
        try (HttpResponse response = HttpRequest.get(url).timeout(5000).execute()) {
            if (!response.isOk()) throw new IllegalArgumentException("upstream status");
            return parse(response.body(), code);
        } catch (Exception error) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "日 K 行情获取失败，请稍后重试");
        }
    }

    static List<IndexChartVO.Candle> parse(String body, String code) {
        JSONObject root = JSON.parseObject(body);
        if (root == null || root.getIntValue("code") != 0 || root.getJSONObject("data") == null) {
            throw new IllegalArgumentException("invalid行情响应");
        }
        JSONObject data = root.getJSONObject("data").getJSONObject(code);
        if (data == null) throw new IllegalArgumentException("missing index");
        JSONArray rows = data.getJSONArray("day");
        if (rows == null) rows = data.getJSONArray("qfqday");
        if (rows == null || rows.size() > 250) throw new IllegalArgumentException("invalid candles");
        List<IndexChartVO.Candle> candles = new ArrayList<>();
        String previous = "";
        for (int i = 0; i < rows.size(); i++) {
            JSONArray row = rows.getJSONArray(i);
            if (row == null || row.size() < 6) throw new IllegalArgumentException("invalid row");
            IndexChartVO.Candle candle = new IndexChartVO.Candle();
            candle.setDate(LocalDate.parse(row.getString(0)).toString());
            candle.setOpen(row.getBigDecimal(1));
            candle.setClose(row.getBigDecimal(2));
            candle.setHigh(row.getBigDecimal(3));
            candle.setLow(row.getBigDecimal(4));
            candle.setVolume(row.getBigDecimal(5));
            if (candle.getOpen() == null || candle.getClose() == null || candle.getHigh() == null
                    || candle.getLow() == null || candle.getVolume() == null
                    || candle.getLow().signum() <= 0 || candle.getVolume().signum() < 0
                    || candle.getHigh().compareTo(candle.getOpen().max(candle.getClose())) < 0
                    || candle.getLow().compareTo(candle.getOpen().min(candle.getClose())) > 0
                    || candle.getDate().compareTo(previous) <= 0) throw new IllegalArgumentException("invalid OHLC");
            previous = candle.getDate();
            candles.add(candle);
        }
        return candles;
    }

    private IndexTransactionVO marker(IndexTradeRecord record, String code) {
        IndexTransactionVO vo = new IndexTransactionVO();
        vo.setId(record.getId()); vo.setIndexCode(code); vo.setIndexName(FundConstants.getIndexName(code));
        vo.setTradeType(record.getTradeType()); vo.setAmount(record.getAmount()); vo.setNav(record.getNav());
        vo.setShares(record.getShares()); vo.setStatus(record.getStatus());
        vo.setStatusName(record.getStatus() == 1 ? "已完成" : "待结算");
        vo.setCreateTime(record.getCreateTime().toInstant().atZone(SHANGHAI).toLocalDateTime());
        return vo;
    }
}
