package com.cong.fishisland.service;

import javax.annotation.Resource;

import com.cong.fishisland.constant.ActionTypeConstant;
import com.cong.fishisland.constant.SourceTypeConstant;
import com.cong.fishisland.model.entity.event.EventRemind;
import com.cong.fishisland.service.event.EventRemindHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

/**
 * 用户服务测试
 * <p>
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@SpringBootTest
public class UserServiceTest {

    @Resource
    private UserService userService;
    @Resource
    private EventRemindHandler eventRemindHandler;
    @Resource
    private EventRemindService eventRemindService;

    @Test
    void testEvent() {
//        eventRemindHandler.handleSystemMessage(1L, "恭喜获得宠物排行榜称号！");
        EventRemind event = new EventRemind();
        event.setAction(ActionTypeConstant.SYSTEM);
        event.setSourceType(SourceTypeConstant.SYSTEM);
        event.setSourceContent("");
        event.setRecipientId(1L);
        event.setRemindTime(new Date());
        eventRemindService.save(event);
    }

    @Test
    void userRegister() {
        String userAccount = "cong";
        String userPassword = "";
        String checkPassword = "123456";
        try {
            long result = userService.userRegister(userAccount, userPassword, checkPassword);
            Assertions.assertEquals(-1, result);
            userAccount = "yu";
            result = userService.userRegister(userAccount, userPassword, checkPassword);
            Assertions.assertEquals(-1, result);
        } catch (Exception e) {

        }
    }
}
