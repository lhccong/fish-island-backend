package com.cong.fishisland.service.impl.emoticon;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cong.fishisland.mapper.emoticon.EmoticonFavourMapper;
import com.cong.fishisland.model.dto.emoticon.EmoticonFavourQueryRequest;
import com.cong.fishisland.model.entity.emoticon.EmoticonFavour;
import com.cong.fishisland.model.entity.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmoticonFavourServiceTest {
    private final EmoticonFavourMapper mapper = mock(EmoticonFavourMapper.class);
    private final EmoticonFavourServiceImpl service = new EmoticonFavourServiceImpl();

    EmoticonFavourServiceTest() {
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void listFiltersOnlyAccountTextAndTreatsWildcardsLiterally() {
        EmoticonFavourQueryRequest request = new EmoticonFavourQueryRequest();
        request.setKeyword("  摸鱼_%!  ");
        service.listForUser(42L, request);
        ArgumentCaptor<QueryWrapper<EmoticonFavour>> captured =
                (ArgumentCaptor) ArgumentCaptor.forClass(QueryWrapper.class);
        verify(mapper).selectPage(any(), captured.capture());

        QueryWrapper<EmoticonFavour> query = captured.getValue();
        String sql = query.getSqlSegment();
        assertTrue(sql.contains("userId ="));
        assertTrue(sql.contains("ocrText LIKE"));
        assertTrue(sql.contains("ESCAPE '!'"));
        assertTrue(sql.contains("ORDER BY createTime DESC,id DESC"));
        assertTrue(query.getParamNameValuePairs().containsValue(42L));
        assertTrue(query.getParamNameValuePairs().containsValue("%摸鱼!_!%!!%"));
    }

    @Test
    void newFavouriteAndOcrTaskAreInsertedTogether() {
        User user = new User();
        user.setId(42L);
        when(mapper.insert(any())).thenReturn(1);
        String url = "https://example.test/emoji.png";
        assertTrue(service.addEmoticonFavour(url, user));

        ArgumentCaptor<EmoticonFavour> captured = ArgumentCaptor.forClass(EmoticonFavour.class);
        verify(mapper).insert(captured.capture());
        assertEquals(url, captured.getValue().getEmoticonSrc());
        assertEquals(Long.valueOf(42L), captured.getValue().getUserId());
        assertNotNull(captured.getValue().getOcrNextAttemptAt());
        assertNull(captured.getValue().getOcrText());
    }
}
