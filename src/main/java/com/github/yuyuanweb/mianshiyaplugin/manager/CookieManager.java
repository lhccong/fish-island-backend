package com.github.yuyuanweb.mianshiyaplugin.manager;

import cn.hutool.core.util.StrUtil;
import com.github.yuyuanweb.mianshiyaplugin.config.ApiConfig;
import com.github.yuyuanweb.mianshiyaplugin.config.GlobalState;
import com.github.yuyuanweb.mianshiyaplugin.constant.KeyConstant;
import com.github.yuyuanweb.mianshiyaplugin.model.response.User;
import com.github.yuyuanweb.mianshiyaplugin.utils.PanelUtil;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import lombok.extern.slf4j.Slf4j;
import org.cef.network.CefCookieManager;

import javax.swing.*;
import java.io.IOException;

/**
 * Cookie 管理器
 *
 * @author pine
 */
@Slf4j
public class CookieManager {

    /**
     * 获取当前登录用户
     */
    public static User getLoginUser() {
        User user = null;
        try {
            user = ApiConfig.mianShiYaApi.getLoginUser().execute().body().getData();
        } catch (IOException e) {
            log.error("Failed to get login user", e);
            // throw new RuntimeException(e);
        }
        return user;
    }

    public static void handleCookie(CefCookieManager cefCookieManager, Runnable afterLogin) {
        cefCookieManager.visitAllCookies((cefCookie, count, total, boolRef) -> {
            String session = "SESSION";
            if (!session.equals(cefCookie.name) || StrUtil.isBlank(cefCookie.value)) {
                return false;
            }
            GlobalState globalState = GlobalState.getInstance();
            String oldCookie = globalState.getSavedCookie();
            String newCookie = session + "=" + cefCookie.value;
            if (oldCookie.equals(newCookie)) {
                return false;
            }
            globalState.saveCookie(newCookie);
            User loginUser = CookieManager.getLoginUser();
            if (loginUser != null) {
                // Ensure the UI updates are done on the Event Dispatch Thread
                globalState.saveUser(loginUser);
                ActionManager actionManager = ActionManager.getInstance();
                DefaultActionGroup actionGroup = (DefaultActionGroup) actionManager.getAction(KeyConstant.ACTION_BAR);
                PanelUtil.modifyActionGroupWhenLogin(actionGroup, loginUser);
                SwingUtilities.invokeLater(afterLogin);
            } else {
                globalState.removeSavedCookie();
                globalState.removeSavedUser();
            }
            return false;
        });
    }

}
