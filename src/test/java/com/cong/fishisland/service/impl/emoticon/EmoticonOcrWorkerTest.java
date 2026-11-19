package com.cong.fishisland.service.impl.emoticon;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cong.fishisland.mapper.emoticon.EmoticonFavourMapper;
import com.cong.fishisland.model.entity.emoticon.EmoticonFavour;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmoticonOcrWorkerTest {
    private static final String IMAGE_URL = "https://example.test/emoji.png";
    private final EmoticonFavourMapper mapper = mock(EmoticonFavourMapper.class);
    private final EmoticonOcr ocr = mock(EmoticonOcr.class);
    private final EmoticonOcrWorker worker = new EmoticonOcrWorker(mapper, ocr);

    @BeforeAll
    static void initTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(false);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "emoticon"), EmoticonFavour.class);
    }

    @Test
    void writesRecognizedTextAndClearsPendingTask() throws Exception {
        mockPending(0);
        when(mapper.update(isNull(), any())).thenReturn(1);
        when(ocr.recognize(IMAGE_URL)).thenReturn("一起摸鱼");
        worker.processNext();

        LambdaUpdateWrapper<EmoticonFavour> completed = captureCompletion();
        assertTrue(completed.getParamNameValuePairs().containsValue("一起摸鱼"));
        assertTrue(completed.getParamNameValuePairs().containsValue(null));
    }

    @Test
    void retriesTransientFailureThenStopsAtLimit() throws Exception {
        mockPending(1);
        when(mapper.update(isNull(), any())).thenReturn(1);
        when(ocr.recognize(IMAGE_URL)).thenThrow(new IOException());
        worker.processNext();
        LambdaUpdateWrapper<EmoticonFavour> retry = captureCompletion();
        assertTrue(retry.getParamNameValuePairs().containsValue(2));
        assertEquals(2, retry.getParamNameValuePairs().values().stream().filter(Date.class::isInstance).count());

        clearInvocations(mapper);
        mockPending(2);
        worker.processNext();
        LambdaUpdateWrapper<EmoticonFavour> exhausted = captureCompletion();
        assertTrue(exhausted.getParamNameValuePairs().containsValue(3));
        assertEquals(1, exhausted.getParamNameValuePairs().values().stream().filter(Date.class::isInstance).count());
    }

    @Test
    void skipsRecognitionIfAnotherWorkerClaimedTask() {
        mockPending(0);
        worker.processNext();
        verifyNoInteractions(ocr);
        verify(mapper).update(isNull(), any());
    }

    private void mockPending(int attempts) {
        EmoticonFavour row = new EmoticonFavour();
        row.setId(43L);
        row.setEmoticonSrc(IMAGE_URL);
        row.setOcrAttempts(attempts);
        row.setOcrNextAttemptAt(new Date(System.currentTimeMillis() - 1000));
        when(mapper.selectOne(any())).thenReturn(row);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private LambdaUpdateWrapper<EmoticonFavour> captureCompletion() {
        ArgumentCaptor<LambdaUpdateWrapper<EmoticonFavour>> captured =
                (ArgumentCaptor) ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(mapper, times(2)).update(isNull(), captured.capture());
        LambdaUpdateWrapper<EmoticonFavour> completed = captured.getAllValues().get(1);
        assertTrue(completed.getSqlSegment().contains("ocrNextAttemptAt ="));
        assertTrue(completed.getSqlSet().contains("ocrText"));
        assertTrue(completed.getParamNameValuePairs().containsValue(43L));
        return completed;
    }
}
