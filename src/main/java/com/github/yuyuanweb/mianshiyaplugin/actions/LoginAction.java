package com.github.yuyuanweb.mianshiyaplugin.actions;

import cn.hutool.core.util.StrUtil;
import com.github.yuyuanweb.mianshiyaplugin.config.GlobalState;
import com.github.yuyuanweb.mianshiyaplugin.manager.CookieManager;
import com.github.yuyuanweb.mianshiyaplugin.model.response.User;
import com.github.yuyuanweb.mianshiyaplugin.utils.PanelUtil;
import com.github.yuyuanweb.mianshiyaplugin.view.LoginPanel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * 登录
 *
 * @author pine
 */
public class LoginAction extends AnAction implements DumbAware {

    private final DefaultActionGroup actionGroup;

    // 构造函数
    public LoginAction(String text, Icon icon, DefaultActionGroup actionGroup) {
        // Action 名称
        super(text, text, icon);
        this.actionGroup = actionGroup;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        LoginPanel loginPanel = new LoginPanel(ProjectManager.getInstance().getDefaultProject());
        loginPanel.showAndGet();

        GlobalState globalState = GlobalState.getInstance();
        String cookie = globalState.getSavedCookie();
        if (StrUtil.isBlank(cookie)) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            User loginUser = CookieManager.getLoginUser();
            if (loginUser == null) {
                return;
            }

            PanelUtil.modifyActionGroupWhenLogin(actionGroup, loginUser);
        });

    }
}
