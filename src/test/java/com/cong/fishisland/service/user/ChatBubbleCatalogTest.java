package com.cong.fishisland.service.user;

import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.user.ChatBubbleProductMapper;
import com.cong.fishisland.model.entity.user.ChatBubbleProduct;
import com.cong.fishisland.service.impl.user.ChatBubbleCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatBubbleCatalogTest {
    private static final String IMAGE = "https://assets.test/cloud.svg";
    private final ChatBubbleProductMapper mapper = mock(ChatBubbleProductMapper.class);
    private final ChatBubbleCatalog catalog = new ChatBubbleCatalog(mapper);

    @Test
    void createsNewStyleWithArtAndUpdatesPriceWithoutChangingArt() {
        when(mapper.create(any())).thenReturn(1);
        ChatBubbleProduct product = catalog.create("cloud-kite", " 云上风筝 ", 425, IMAGE, null, null);
        assertEquals("云上风筝", product.getName());
        assertEquals(IMAGE, product.artwork().getImageUrl());
        assertEquals("theme", product.artwork().getTextTone());
        verify(mapper).create(product);
        product.setPoints(460);
        when(mapper.find("cloud-kite")).thenReturn(product);
        assertEquals(product, catalog.setPrice("cloud-kite", 460));
        verify(mapper).setPrice("cloud-kite", 460);
    }

    @Test
    void rejectsInvalidStylesPricesAndUrls() {
        assertThrows(BusinessException.class, () -> catalog.create("../bad", "风筝", 300, IMAGE, null, null));
        assertThrows(BusinessException.class, () -> catalog.setPrice("cloud-kite", 0));
        for (String badUrl : new String[]{"http://assets.test/bubble.svg", "https://user@assets.test/a.svg", "javascript:alert(1)"}) {
            assertThrows(BusinessException.class, () -> catalog.create("cloud-kite", "风筝", 300, badUrl, null, null));
        }
        verifyNoInteractions(mapper);
        when(mapper.create(any())).thenThrow(new DuplicateKeyException("duplicate"));
        assertThrows(BusinessException.class, () -> catalog.create("cloud-kite", "风筝", 300, IMAGE, null, null));
    }
}
