package com.cong.fishisland.controller.user;

import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.user.UserCardVO;
import com.cong.fishisland.model.vo.user.UserVO;
import com.cong.fishisland.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserCardControllerTest {

    @Test
    void includesCurrentAndOwnedTitlesWithExistingPublicProfile() {
        UserService service = mock(UserService.class);
        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", service);
        User user = new User();
        user.setId(123L);
        user.setTitleId(7L);
        user.setTitleIdList("[\"7\",\"2\"]");
        user.setMbti("INFP");
        UserVO publicProfile = new UserVO();
        publicProfile.setId(123L);
        publicProfile.setFollowerCount(5L);
        when(service.getById(123L)).thenReturn(user);
        when(service.getUserVO(user)).thenReturn(publicProfile);

        UserCardVO card = controller.getUserVoById(123L).getData();
        assertEquals(Long.valueOf(7), card.getTitleId());
        assertEquals("[\"7\",\"2\"]", card.getTitleIdList());
        assertEquals(Long.valueOf(5), card.getFollowerCount());
        assertEquals("INFP", card.getMbti());

        user.setTitleId(null);
        user.setTitleIdList(" ");
        UserCardVO empty = controller.getUserVoById(123L).getData();
        assertEquals(Long.valueOf(0), empty.getTitleId());
        assertEquals("[]", empty.getTitleIdList());
    }
}
