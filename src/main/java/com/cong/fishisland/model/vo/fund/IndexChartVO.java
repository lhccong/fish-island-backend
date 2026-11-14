package com.cong.fishisland.model.vo.fund;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class IndexChartVO {
    private List<Candle> candles;
    private List<IndexTransactionVO> trades;
    private boolean truncated;

    @Data
    public static class Candle {
        private String date;
        private BigDecimal open;
        private BigDecimal close;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal volume;
    }
}
