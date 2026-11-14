package com.cong.fishisland.service.fund;

import com.cong.fishisland.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IndexChartServiceTest {
    private String payload(String rows) { return "{\"code\":0,\"data\":{\"sh000001\":{\"day\":" + rows + "}}}"; }
    @Test void parsesActualProviderOrder() {
        String body = payload("[[\"2026-11-12\",\"3673.06\",\"3655.83\",\"3699.58\",\"3647.44\",\"496533033\"]]");
        assertEquals("3655.83", IndexChartService.parse(body,"sh000001").get(0).getClose().toPlainString());
        assertEquals("3699.58", IndexChartService.parse(body,"sh000001").get(0).getHigh().toPlainString());
    }
    @Test void rejectsImpossibleOrDuplicateCandles() {
        assertThrows(IllegalArgumentException.class, () -> IndexChartService.parse(payload("[[\"2026-11-12\",3,4,2,1,0]]"), "sh000001"));
        assertThrows(IllegalArgumentException.class, () -> IndexChartService.parse(payload("[[\"2026-11-12\",3,4,5,2,0],[\"2026-11-12\",3,4,5,2,0]]"), "sh000001"));
    }
    @Test void rejectsArbitraryCodeAndPeriodBeforeNetwork() {
        IndexChartService service = new IndexChartService();
        assertThrows(BusinessException.class, () -> service.chart(1L, "https://example.com",60));
        assertThrows(BusinessException.class, () -> service.chart(1L, "sh000001",100000));
    }

    @Test void isolatesAccountMarkersWhileSharingPublicCandleCache() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), ""),
                com.cong.fishisland.model.entity.fund.IndexTradeRecord.class);
        com.cong.fishisland.mapper.fund.IndexTradeMapper mapper = org.mockito.Mockito.mock(com.cong.fishisland.mapper.fund.IndexTradeMapper.class);
        IndexChartService service = new IndexChartService() {
            @Override protected java.util.List<com.cong.fishisland.model.vo.fund.IndexChartVO.Candle> fetch(String code, int days) {
                return parse(payload("[[\"2026-11-12\",3,4,5,2,0]]"), code);
            }
        };
        org.springframework.test.util.ReflectionTestUtils.setField(service, "tradeMapper", mapper);
        org.mockito.Mockito.when(mapper.selectList(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<?> query = invocation.getArgument(0);
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("userId")); assertTrue(sql.contains("indexCode"));
            assertTrue(sql.contains("createTime")); assertTrue(sql.contains("LIMIT 5001"));
            return java.util.Collections.emptyList();
        });
        service.chart(11L, "sh000001", 60);
        service.chart(22L, "sh000001", 60);
        org.mockito.ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.Wrapper> capture = org.mockito.ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        org.mockito.Mockito.verify(mapper, org.mockito.Mockito.times(2)).selectList(capture.capture());
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<?> first = (com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<?>) capture.getAllValues().get(0);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<?> second = (com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<?>) capture.getAllValues().get(1);
        assertTrue(first.getParamNameValuePairs().containsValue(11L));
        assertFalse(first.getParamNameValuePairs().containsValue(22L));
        assertTrue(second.getParamNameValuePairs().containsValue(22L));
    }
}
