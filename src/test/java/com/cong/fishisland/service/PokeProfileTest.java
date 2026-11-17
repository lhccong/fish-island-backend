package com.cong.fishisland.service;

import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.controller.user.UserController;
import com.cong.fishisland.model.dto.user.UserUpdateMyRequest;
import com.cong.fishisland.model.entity.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PokeProfileTest {
    @Test void ordinaryUserCanClearSuffixWithoutChangingOtherProfileFields() {
        UserController controller = new UserController();
        UserService users = mock(UserService.class);
        ReflectionTestUtils.setField(controller, "userService", users);
        User login = new User();login.setId(123L);login.setUserName("合成岛友");login.setUserRole("user");
        when(users.getLoginUser()).thenReturn(login);when(users.updateById(any(User.class))).thenReturn(true);
        UserUpdateMyRequest input = new UserUpdateMyRequest();input.setUserName("合成岛友");input.setPokeSuffix("");
        controller.updateMyUser(input);
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).updateById(saved.capture());
        assertEquals(Long.valueOf(123L), saved.getValue().getId());
        assertEquals("", saved.getValue().getPokeSuffix());
        assertNull(saved.getValue().getUserName());
    }
    @Test void rejectsLongAndMultilineSuffixBeforeAnyWrite() {
        UserController controller = new UserController();
        for (String suffix : new String[]{new String(new char[31]).replace('\0', 'a'), "换\n行", "控制\u0001字"}) {
            UserUpdateMyRequest input = new UserUpdateMyRequest();
            input.setUserName("合成岛友");input.setPokeSuffix(suffix);
            assertThrows(BusinessException.class, () -> controller.updateMyUser(input));
        }
    }

    @Test void savesThirtyCharactersClearsEmptyAndPreservesOmittedField() {
        UserController controller = new UserController();
        UserService users = mock(UserService.class);
        ReflectionTestUtils.setField(controller, "userService", users);
        User login = new User();login.setId(123L);login.setUserName("合成岛友");login.setUserRole("admin");
        when(users.getLoginUser()).thenReturn(login);when(users.updateById(any(User.class))).thenReturn(true);
        for (String suffix : new String[]{new String(new char[30]).replace('\0', 'a'), "", null}) {
            UserUpdateMyRequest input = new UserUpdateMyRequest();input.setUserName("合成岛友");input.setPokeSuffix(suffix);
            controller.updateMyUser(input);
            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(users).updateById(saved.capture());
            assertEquals(suffix, saved.getValue().getPokeSuffix());
            clearInvocations(users);
        }
    }
}
