package com.cong.fishisland.controller.user;

import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.user.UserUpdateMyRequest;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserProfileMbtiTest {

    private final UserService service = mock(UserService.class);
    private final UserController controller = new UserController();
    private final UserUpdateMyRequest request = new UserUpdateMyRequest();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "userService", service);
        User account = new User();
        account.setId(123L);
        account.setUserName("测试用户");
        account.setUserRole("user");
        when(service.getLoginUser()).thenReturn(account);
        when(service.updateById(any(User.class))).thenReturn(true);
        request.setUserName(account.getUserName());
    }

    @Test
    void savesWithoutRenamingOrChangingOtherProfileFields() {
        assertEquals("INFP", savedMbti("INFP"));
    }

    @Test
    void writesAnEmptyStringWhenClearing() {
        assertEquals("", savedMbti(""));
    }

    @Test
    void leavesMbtiOutOfAProfileUpdateFromAnOlderClient() {
        request.setUserProfile("合成简介");
        assertNull(savedMbti(null));
    }

    @Test
    void rejectsUnsupportedTypesBeforeAnyWrite() {
        for (String value : new String[]{"INFJ-A", "infj", "XXXX", " "}) {
            request.setMbti(value);
            BusinessException result = assertThrows(
                    BusinessException.class, () -> controller.updateMyUser(request));
            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), result.getCode());
        }
        verifyNoInteractions(service);
    }

    private String savedMbti(String mbti) {
        request.setMbti(mbti);
        assertEquals(Boolean.TRUE, controller.updateMyUser(request).getData());
        ArgumentCaptor<User> patch = ArgumentCaptor.forClass(User.class);
        verify(service).updateById(patch.capture());
        assertEquals(Long.valueOf(123), patch.getValue().getId());
        return patch.getValue().getMbti();
    }
}
