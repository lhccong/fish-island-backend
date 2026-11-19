package com.cong.fishisland.service.user;

import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.GlobalExceptionHandler;
import com.cong.fishisland.controller.user.ChatBubbleController;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.user.ChatBubbleVO;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.impl.user.ChatBubbleCatalog;
import com.cong.fishisland.service.impl.user.ChatBubbleService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChatBubbleControllerTest {
    private final UserService users = mock(UserService.class);
    private final ChatBubbleService bubbles = mock(ChatBubbleService.class);
    private final ChatBubbleCatalog catalog = mock(ChatBubbleCatalog.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ChatBubbleController(bubbles, users, catalog))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void pathsUseApiContextAndMutationsUseLoggedInAccount() throws Exception {
        User user = new User();
        user.setId(123L);
        when(users.getLoginUser()).thenReturn(user);
        when(bubbles.list(123L)).thenReturn(Collections.singletonList(
                new ChatBubbleVO("berry-cat-paw", "草莓猫爪", 300, 30, null, false, null)));

        mvc.perform(get("/api/chat/bubble/list").contextPath("/api"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].points").value(300));
        mvc.perform(post("/api/chat/bubble/exchange").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"bubbleId\":\"berry-cat-paw\",\"expectedPoints\":300}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        mvc.perform(post("/api/chat/bubble/equip").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"bubbleId\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        verify(bubbles).exchange(123L, "berry-cat-paw", 300);
        verify(bubbles).equip(123L, null);
    }

    @Test
    void catalogMutationsRequireAdmin() throws Exception {
        User user = new User();
        when(users.getLoginUser()).thenReturn(user);
        String request = "{\"id\":\"kite\",\"name\":\"风筝\",\"points\":420,\"imageUrl\":\"https://assets.test/kite.svg\"}";
        for (String action : new String[]{"create", "price"}) {
            mvc.perform(post("/api/chat/bubble/admin/" + action).contextPath("/api")
                            .contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(jsonPath("$.code").value(40101));
        }
        verifyNoInteractions(catalog);
        when(users.isAdmin(user)).thenReturn(true);
        for (String action : new String[]{"create", "price"}) {
            mvc.perform(post("/api/chat/bubble/admin/" + action).contextPath("/api")
                            .contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(jsonPath("$.code").value(0));
        }
        verify(catalog).create("kite", "风筝", 420, "https://assets.test/kite.svg", null, null);
        verify(catalog).setPrice("kite", 420);
    }

    @Test
    void guestCannotRedeem() throws Exception {
        when(users.getLoginUser()).thenThrow(new BusinessException(ErrorCode.NOT_LOGIN_ERROR));
        mvc.perform(post("/api/chat/bubble/exchange").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"bubbleId\":\"berry-cat-paw\"}"))
                .andExpect(jsonPath("$.code").value(40100));
        verifyNoInteractions(bubbles);
    }
}
